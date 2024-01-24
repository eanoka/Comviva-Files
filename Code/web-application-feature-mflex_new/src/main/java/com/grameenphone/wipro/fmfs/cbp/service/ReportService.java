package com.grameenphone.wipro.fmfs.cbp.service;

import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Bill;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Company;
import com.grameenphone.wipro.fmfs.cbp.model.orm.report.Transaction;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.BillExcelReport;
import com.grameenphone.wipro.fmfs.cbp.model.view.report.DetailReportRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.report.PaginatedReportData;
import com.grameenphone.wipro.fmfs.cbp.model.view.report.VatReportRequest;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.BillRepository;
import com.grameenphone.wipro.utility.KV;
import com.grameenphone.wipro.utility.common.DateUtil;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.MapUtil;
import com.grameenphone.wipro.utility.common.NumberUtil;
import com.grameenphone.wipro.utility.excel.ExcelWriter;
import com.grameenphone.wipro.utility.orm.HibernateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {
    @Value("${mobicashweb.sms.send.url}")
    String smsSendURL;

    @Value("${mobicashweb.sms.send.timeout}")
    Integer smsSendTimeout;

    @Value("${front.report.max.duration.months}")
    Integer maximumReportDurationMonths;
    
    @Autowired
    BillRepository billRepository;

    private final static Logger logger = LoggerFactory.getLogger(ReportService.class);

    public static List<String> commonHeaders = Arrays.asList("id", "initiator", "company.name", "accountNo", "billNo", "amount", "paymentDate", "thirdPartyTxnid", "mfsTxnid", "reversalTxnid", "status", "transactionType", "channel", "payerMsisdn", "customerMsisdn", "responseCode", "serviceCharge", "paidAmount");

    public static List<String> allHeaders = Arrays.asList("id", "initiator", "paymentDate", "company.name", "customerMsisdn", "accountNo", "billNo", "amount", "custom:vat", "serviceCharge", "paidAmount", "thirdPartyTxnid", "mfsTxnid", "reversalTxnid", "tokenNo", "seqNo", "status");

    public List<String> getApplicableHeaders(String type) {
        List<String> clonedCache = new ArrayList<>(allHeaders.subList(0, allHeaders.size()));
        if(type.equals("detail")) {
            clonedCache.remove("tokenNo");
            clonedCache.remove("seqNo");
        }
        return clonedCache;
    }

    private String getCompanyCode(Long companyId) {
        if(companyId == null) {
            return null;
        }
        return (String)CrudDao.get(Company.class).query().eq("id", companyId).selectOne("code");
    }

    private List<String> getCompanyCodes(Long categoryId) {
        if(categoryId == null) {
            return null;
        }
        return (List)CrudDao.get(Company.class).query().eq("category.id", categoryId).selectAll("code");
    }

    private void applyFromInFilter(Map<String, Object> filterParams, StringBuffer hqlBuffer, Date from) {
        hqlBuffer.append(" and paymentDate >= :fdate");
        filterParams.put("fdate", DateUtil.getStartOfTheDay(from));
    }

    private void applyToInFilter(Map<String, Object> filterParams, StringBuffer hqlBuffer, Date to) {
        hqlBuffer.append(" and paymentDate <= :edate");
        filterParams.put("edate", DateUtil.getEndOfTheDay(to));
    }

    private void applyFilterInHql(Long categoryId, String companyCode, StringBuffer hqlBuffer, Map<String, Object> filterParams, Date from, Date to) {
        if(companyCode != null) {
            hqlBuffer.append(" and T.company.code = :utility_code ");
            filterParams.put("utility_code", companyCode);
        } else if(categoryId != null) {
            hqlBuffer.append(" and T.company.code in (:utility_codes) ");
            filterParams.put("utility_codes", getCompanyCodes(categoryId));
        }

        applyFromInFilter(filterParams, hqlBuffer, from);
        applyToInFilter(filterParams, hqlBuffer, to);
    }

    private String getProjectionHql(List<String> headers) {
        StringBuffer projectionBuffer = new StringBuffer();
        headers.forEach(h -> {
            projectionBuffer.append(", ");
            projectionBuffer.append(getHeaderHql(h));
        });
        return projectionBuffer.substring(2);
    }

    private String getHeaderHql(String header) {
        String asName = header.replaceAll("[\\.:]", "_");
        if (commonHeaders.contains(header)) {
            return header + " as " + asName;
        }
        return "(select value from TransactionExtraData E where E.transaction.id = T.id and E.header.name = '" + header + "') as " + asName;
    }

    public PaginatedReportData getDetailReportData(DetailReportRequest request, boolean allData) {
        if(request.end == null) {
            request.end = new Date();
        }
        if(request.end.getTime() - request.start.getTime() > 6 * 30 * 24 * 60 * 60 * 1000L) {
            throw new AppRuntimeException("Duration can't be greater than " + maximumReportDurationMonths + " months", 422);
        }

        PaginatedReportData tableData = new PaginatedReportData();
        StringBuffer hqlBuffer = new StringBuffer();
        hqlBuffer.append(" from Transaction T where T.channel = 'CBP' ");
        Map<String, Object> queryParams = new LinkedHashMap<>();

        SessionObject session = SessionAttributes.current();
        String accountToLookup = null;
        List<String> initiatorsToLookup = new ArrayList<>();
        String accountName;
        Map<Long, String> subNames = new LinkedHashMap<>();
        if(session.IS_GP) {
            accountToLookup = "" + request.account;
            accountName = (String)CrudDao.get(Client.class).query().eq("id", request.account).selectOne("name");
        } else {
            accountName = null;
            Client client = session.getUser().getClient();
            String accountId = "" + client.getId();
            HibernateUtil.initializeProxy(client.getClientDivisions()).stream().map(d -> {
                Long id = d.getId();
                subNames.put(id, d.getName());
                return id;
            }).collect(Collectors.toList());
            if(!session.getUser().isAllowAllDivision()) {
                List<Long> allowedIds = HibernateUtil.initializeProxy(session.getUser().getClientDivisions()).stream().map(d -> d.getId()).collect(Collectors.toList());
                (request.subAccount == null || request.subAccount.length == 0 ? allowedIds : ((Function<List<Long>, List<Long>>)((_subAccountIds) -> {_subAccountIds.retainAll(Arrays.asList(request.subAccount)); return allowedIds;})).apply(allowedIds)).forEach(s -> {
                    initiatorsToLookup.add(accountId + ":" + s);
                });
            } else if(request.subAccount == null || request.subAccount.length == 0) {
                accountToLookup = accountId;
            } else {
                for (long sub : request.subAccount) {
                    initiatorsToLookup.add(accountId + ":" + sub);
                }
            }
        }
        if(accountToLookup != null) {
            hqlBuffer.append("and T.initiator like :account ");
            queryParams.put("account", accountToLookup + ":%");
        } else {
            hqlBuffer.append("and T.initiator in :subs ");
            queryParams.put("subs", initiatorsToLookup);
        }
        if(request.accNo != null) {
            hqlBuffer.append("and T.accountNo = :accNo ");
            queryParams.put("accNo", request.accNo);
        }

        applyFilterInHql(request.category, getCompanyCode(request.company), hqlBuffer, queryParams, request.start, request.end);

        String hql = hqlBuffer.toString();
        long count = 0;
        if(!allData) {
            count = tableData.total = (Long) CrudDao.get(Transaction.class).executeQuery("select count(*) " + hql, 0, -1, queryParams).get(0);
            if (count <= request.offset) {
                request.offset = (long) (Math.ceil(count / (double) request.perPage) - 1) * request.perPage;
            }
            tableData.offset = request.offset < 0 ? 0 : request.offset;
        } else {
            request.offset = 0;
            request.perPage = -1;
        }
        tableData.records = !allData && 0 == count ? new ArrayList<>() : (List)CrudDao.get(Transaction.class).executeQuery("select new Map(" + getProjectionHql(getApplicableHeaders(request.type)) + ") " + hql, request.offset, request.perPage, queryParams);
        tableData.records.forEach(r -> {
            r.put("account", accountName);
            r.put("subAccount", subNames.get(Long.parseLong(((String)r.get("initiator")).split(":")[1])));
        });
        return tableData;
    }

    @Transactional(readOnly = true)
    public String sendTokenSMS(Long transactionId) {
        Transaction transaction = CrudDao.get(Transaction.class).findOne(transactionId);
        String customer = transaction.customerMsisdn;
        String token = transaction.extraData.stream().filter(f -> f.header.getName().equals("tokenNo")).findFirst().get().value;
        HttpClient httpClient = new HttpClient(smsSendTimeout);
        String response = null;
        try {
            String params = HttpClient.serializeMap(new HashMap() {
                {
                    put("customer", customer);
                    put("token", token);
                }
            });
            logger.debug("Calling mobicashweb to send sms to customer:" + customer + " with token:" + token);
            httpClient.get(smsSendURL + "?" + params);
        } catch (Exception ex) {
            logger.error("Failed to send sms to customer:" + customer + " with token:" + token);
            response = ex.getMessage();
        }
        return response;
    }

    public void downloadAsXls(DetailReportRequest request, OutputStream outputStream) throws IOException {
        List<Map<String, Object>> bills = getDetailReportData(request, true).records;
        Map<String, String> headers = MapUtil.of(new KV<>(SessionAttributes.current().IS_GP ? "account" : "subAccount", SessionAttributes.current().IS_GP ? "Account" : "Sub Account"), new KV<>("paymentDate", "Transaction Date"), new KV<>("company_name", "Biller Name"), new KV<>("customerMsisdn", "MSISDN"), new KV<>("accountNo", "Meter/Account"), new KV<>("billNo", "Bill Number"), new KV<>("amount", "Bill Amount"), new KV<>("detail".equals(request.type) ? "custom_vat" : null, "Vat"), new KV<>("serviceCharge", "Service Charge"), new KV<>("paidAmount", "Paid Amount"), new KV<>("thirdPartyTxnid", "Biller Transaction Id"), new KV<>("mfsTxnid", "GPAY Transaction Id"), new KV<>("reversalTxnid", "Reversal Transaction Id"), new KV<>("detail".equals(request.type) ? null : "tokenNo", "Token No."), new KV<>("detail".equals(request.type) ? null : "seqNo", "Seq. No."), new KV<>("status", "Status"));
        ExcelWriter<Map<String, Object>> excel = new ExcelWriter<>(headers) {};
        SimpleDateFormat dateFormat = new SimpleDateFormat(" MMM YYYY");
        excel.setFormatter("paymentDate", (date) -> {
            Calendar d = Calendar.getInstance();
            d.setTime((Date)date);
            int day = d.get(Calendar.DATE);
            return day + NumberUtil.getOrdinal(day) + dateFormat.format(date);
        });
        excel.setFormatter("amount", (amount) -> NumberUtil.toBdFormat(Double.parseDouble((String)amount)));
        excel.setFormatter("custom_vat", (amount) -> NumberUtil.toBdFormat(Double.parseDouble((String)amount)));
        excel.setFormatter("serviceCharge", (amount) -> NumberUtil.toBdFormat((Double)amount));
        excel.setFormatter("paidAmount", (amount) -> NumberUtil.toBdFormat((Double)amount));
        excel.addHeader();
        excel.write(bills);
        excel.flush(outputStream);
    }

	public PaginatedReportData getVatReportData(VatReportRequest request, boolean allData) {
		// TODO Auto-generated method stub
		billRepository.findByRequestId(0);
		return null;
	}
}