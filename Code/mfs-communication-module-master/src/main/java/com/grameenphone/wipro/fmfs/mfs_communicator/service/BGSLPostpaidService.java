package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.exception.ServiceProcessingError;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.Bill;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.DueBills;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.bgsl.BgslGetBillResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.bgsl.BgslPayBillResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.bgsl.BgslReconcileResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.BillPayServiceStatusRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillFetcher;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayDisputeResolver;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.marshal.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class BGSLPostpaidService implements BillFetcher,
        BillPayer<BGSLPostpaidService.BgslPaymentState, BgslPayBillResponse>, BillPayDisputeResolver {

    protected static final Logger logger = LoggerFactory.getLogger(BGSLPostpaidService.class);
    private static final String COMPANY_CODE = "BGSL";

    @Value("${bgsl_postpaid_get_bill_url}")
    String getBillUrl;

    @Value("${bgsl_postpaid_due_bill_payment_url}")
    String dueBillPaymentUrl;

    @Value("${bgsl_postpaid_advanced_bill_payment_url}")
    String advancedBillPaymentUrl;

    @Value("${bgsl_postpaid_reconciliation_url}")
    String reconciliationUrl;

    @Value("${bgsl_postpaid_proxy_required}")
    Boolean isProxyRequired;

    @Value("${bgsl_postpaid_request_timeout}")
    int timeout;

    @Value("${bgsl_postpaid_authentication}")
    String authentication;

    @Autowired
    BillPayServiceStatusRepository billPayServiceStatusRepo;

    @Autowired
    DisputeService disputeService;

    @Autowired
    PrepaidBillTokenRepository prepaidBillTokenRepo;

    @Bean({"BGSL_Bill_Fetcher", "BGSL_Bill_Payer", "BGSL_BillPayDisputeResolverService"})
    public BGSLPostpaidService alias() {
        return this;
    }

    public class BgslPaymentState extends PaymentState {
        public String customerName;
        public String mobileNo;
    }

    @Override
    public BgslPaymentState getState() {
        return new BgslPaymentState();
    }

    @Override
    public String getCategory() {
        return "GAS";
    }

    @Override
    public PaymentResult handleException(Throwable t) {
        if (t instanceof ValidationException) {
            PaymentResult result = new PaymentResult();
            result.message = t.getMessage();
            result.status = BillPayStatus.FAIL;
            return result;
        }
        return BillPayer.super.handleException(t);
    }

    @Override
    public DueBills fetchDueBills(String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map params) throws ValidationException {
        if (!params.containsKey("mobileNo") || !params.get("mobileNo").toString().matches("\\d{11}")) {
            throw new ValidationException("Invalid mobileNo in param list");
        }
        BgslGetBillResponse getBillResponse = executeBgslGetWebService(
                getBillUrl + consumerId + "/" + params.get("mobileNo"), BgslGetBillResponse.class);

        if (getBillResponse == null) {
            throw new ValidationException("Unable to collect due bill from BGSL.");
        }
        if (getBillResponse.statusCode != 200) {
            throw new ValidationException(getBillResponse.message);
        }
        DueBills dueBills = new DueBills();
        dueBills.company = COMPANY_CODE;
        dueBills.consumerId = consumerId;

        Bill bill = new Bill();
        bill.billNo = StringUtil.generateUniqueReference(String.valueOf(new Random(System.currentTimeMillis()).nextInt(100000)));
        try {
            bill.billDueDate = new SimpleDateFormat("dd-MM-yyyy").parse(getBillResponse.content.get(0).lastPaymentDate);
        } catch (ParseException e) {
            logger.error("Unable to parse date.", e);
        }
        bill.amount = getBillResponse.content.get(0).currentTotal;
        bill.detail = getBillResponse.content.get(0);

        if (bill.amount == 0) {
            bill.serviceCharge = 0.0;
        } else {
            bill.serviceCharge = mfsService.getServiceCharge(msisdn, COMPANY_CODE, bill.amount, wallet_type, channel);
        }
        dueBills.bills.add(bill);

        return dueBills;
    }

    @Override
    public void preparePayment(BgslPaymentState bgslPaymentState, PaymentRequest request) throws HttpErrorResponseException {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        List<BillPayServiceStatus> billPayServiceStatusList = billPayServiceStatusRepo
                .findCurrentDateDisputeTxnByAccountAndAmount(COMPANY_CODE, request.getConsumerId(), request.amount,
                        calendar.getTime());
        if (billPayServiceStatusList.size() > 0) {
            throw new ValidationException("Paying Same Amount Twice on Same Day is not Supported");
        }
        request.amount = Math.ceil(request.amount);
    }

    @Override
    public BgslPayBillResponse pay(BgslPaymentState state, PaymentRequest request, String mfsChannel) {
        state.mobileNo = String.valueOf(request.params.get("mobileNo"));
        state.customerName = String.valueOf(request.params.get("customerName"));
        String url;
        if ((boolean) request.params.get("isAdvance")) {
            url = advancedBillPaymentUrl + request.getConsumerId() + "/" + request.params.get("mobileNo") + "/" + request.amount + "/GP/GPAY";
        } else {
            url = dueBillPaymentUrl + request.getConsumerId() + "/" + request.params.get("mobileNo") + "/" + (int) request.amount;
        }
        try {
            return executeBgslPostWebService(url, BgslPayBillResponse.class);
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public PaymentResult convertToGeneric(BgslPaymentState state, BgslPayBillResponse payBillResponse, PaymentRequest request) {
        PaymentResult result = new PaymentResult();
        String thirdPartyTxStatus = null;
        try {
            Runnable disputePlacer = () -> {
                result.txnId = state.mfsPaymentResponse.txnid;
                result.status = BillPayStatus.DISPUTE;
                result.message = "Your payment has been received. Please contact BGDCL COMMERCIAL DIVISION for further confirmation.";
                disputeService.insertDisputeRecord(state.billPayServiceStatus, request.msisdn, request.customer, null);
            };
            if (payBillResponse != null) {
                switch (payBillResponse.statusCode / 100) {
                    case 2:
                        result.txnId = state.mfsPaymentResponse.txnid;
                        result.status = thirdPartyTxStatus = BillPayStatus.SUCCESS;
                        result.message = "Your due bill is paid successfully. You will get payment confirmation SMS within short time.";
                        return result;
                    case 4:
                        logger.error("Rolling back the transaction based on payment result status code");
                        mfsService.rollbackTransaction(state.mfsPaymentResponse.txnid, request.getCompany() + "BILLPAY", request.wallet_type.equals(WalletType.RET), state.billPayServiceStatus);
                        result.status = thirdPartyTxStatus = state.billPayServiceStatus.getStatus();
                        result.message = "Bill Payment Failed";
                        return result;
                    default:
                        logger.error("Placing the transaction in dispute based on payment result status code");
                        thirdPartyTxStatus = BillPayStatus.DISPUTE;
                        disputePlacer.run();
                        return result;
                }
            } else {
                thirdPartyTxStatus = BillPayStatus.DISPUTE;
                disputePlacer.run();
            }
        } finally {
            if (payBillResponse != null) {
                state.billPayServiceStatus.setThirdPartyTxnid(result.vendorTxnId = payBillResponse.content == null ? null : payBillResponse.content.txId);
                state.billPayServiceStatus.setThirdPartyTxnStatus(payBillResponse.status);
            }
            state.billPayServiceStatus.setStatus(thirdPartyTxStatus);
            billPayServiceStatusRepo.save(state.billPayServiceStatus);
            if (thirdPartyTxStatus == BillPayStatus.DISPUTE || thirdPartyTxStatus == BillPayStatus.SUCCESS) {
                try {
                    PrepaidBillToken prepaidBillToken = preparePrepaidBillToken(state, String.valueOf(request.params.get("isAdvance")));
                    prepaidBillTokenRepo.save(prepaidBillToken);
                } catch (Exception e) {
                    logger.error("Could not insert into token table." + e.getMessage());
                }
            }
        }
        return result;
    }

    private PrepaidBillToken preparePrepaidBillToken(BgslPaymentState state, String isAdvanced) throws JsonProcessingException {
        PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setCompanyCode(COMPANY_CODE);
        prepaidBillToken.setBillPayTableId(state.billPayServiceStatus.getId());

        Map<String, Object> feeMap = new HashMap<>();
        feeMap.put("customerName", state.customerName);
        feeMap.put("mobileNo", state.mobileNo);
        feeMap.put("isAdvance", isAdvanced);
        String fees = Json.toJson(feeMap);
        prepaidBillToken.setFees(fees);

        return prepaidBillToken;
    }

    private <T> T executeBgslGetWebService(String url, Class<T> clazz) {
        HttpClient client = new HttpClient(timeout);
        if (isProxyRequired) {
            client.setDefaultProxy();
        }
        try {
            return client.getForEntity(url, new HashMap<>() {
                {
                    put("Authorization", authentication);
                }
            }, clazz);
        } catch (IOException e) {
            try {
                if (client.getTextResponse() != null) {
                    return Json.fromJson(client.getTextResponse(), clazz);
                }
            } catch (IOException ex) {
                logger.error("Parse Error:: ", ex);
            }
        }
        return null;
    }

    private <T> T executeBgslPostWebService(String url, Class<T> clazz) throws IOException {
        HttpClient client = new HttpClient(timeout);
        if (isProxyRequired) {
            client.setDefaultProxy();
        }
        return client.postForEntity(url, "", new HashMap<>() {
            {
                put("Authorization", authentication);
            }
        }, clazz);
    }

    public void resolveDispute(DisputeTransaction disputeTransaction) {
        Date txdnate = disputeTransaction.getCreationDate();
        if (!LocalDate.now().equals(LocalDate.ofInstant(txdnate.toInstant(), ZoneId.systemDefault()))) {
            throw new ServiceProcessingError("Dispute Transaction is not of same calendar date", 400);
        }
        BgslReconcileResponse reconcileResponse;
        BillPayServiceStatus billPayServiceStatus = disputeTransaction.getBillPayServiceStatus();
        String consutomerCode = disputeTransaction.getBillPayServiceStatus().getAccountNo();
        String reconcileUrl = reconciliationUrl + consutomerCode + "/GP";
        reconcileResponse = executeBgslGetWebService(reconcileUrl, BgslReconcileResponse.class);
        if (reconcileResponse == null || reconcileResponse.statusCode / 100 != 2) {
            logger.error("Unable to get reconciliation response, so leaving the status as is");
            return;
        } else {
            List<String> list;
            String transactionNumber;
            String url = "";
            BgslPayBillResponse payResponse = null;
            BgslReconcileResponse.Content content = reconcileResponse.content.get(0);
            boolean isSuccess = false;
            try {
                if (StringUtils.isNotBlank(content.transaction_amount)) {
                    list = Arrays.asList(content.transaction_amount.split(","));
                    if (list.contains(String.valueOf(disputeTransaction.getAmount()))) {
                        transactionNumber = getValue(content.transaction_number, list.indexOf(String.valueOf(disputeTransaction.getAmount())));
                        billPayServiceStatus.setThirdPartyTxnid(transactionNumber);
                        billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
                        isSuccess = true;
                    }
                }
                if (!isSuccess) {
                    //Will try for a repayment
                    logger.error("Paying again as the transaction not found in response");
                    PrepaidBillToken prepaidBillToken = prepaidBillTokenRepo.findByBillPayTableId(billPayServiceStatus.getId());
                    Map<String, String> fee;
                    try {
                        fee = Json.fromJson(prepaidBillToken.getFees(), Map.class);
                    } catch (Exception e) {
                        throw new ServiceProcessingError("Giving up, as necessary dependent data not available", 400, e); //400 to Give Up next try
                    }
                    String mobileNo = fee.get("mobileNo");
                    boolean isAdvance = Boolean.parseBoolean(fee.get("isAdvance"));
                    int amount = (int) disputeTransaction.getAmount();
                    BgslGetBillResponse getBillResponse = executeBgslGetWebService(getBillUrl + consutomerCode + "/" + mobileNo, BgslGetBillResponse.class);
                    if (getBillResponse == null) {
                        logger.error("Unable to fetch current bill details, so leaving the status as is");
                        return;
                    }
                    if (isAdvance && getBillResponse.content.get(0).currentTotal == 0) {
                        url = advancedBillPaymentUrl + consutomerCode + "/" + mobileNo + "/" + amount + "/GP/GPAY";
                    } else if (!isAdvance && getBillResponse.content.get(0).currentTotal == disputeTransaction.getAmount()) {
                        url = dueBillPaymentUrl + consutomerCode + "/" + mobileNo + "/" + amount;
                    } else {
                        logger.error("Rolling back the transaction. As due amount didn't match");
                        mfsService.rollbackTransaction(billPayServiceStatus.getMfsTxnid(), COMPANY_CODE + "BILLPAY", (billPayServiceStatus.getCustomerMsisdn() != null ? true : false), billPayServiceStatus);
                    }
                    try {
                        payResponse = executeBgslPostWebService(url, BgslPayBillResponse.class);
                    } catch (Exception e) {
                    }

                    if (payResponse == null) {
                        logger.error("Unable to fetch pay bill result again, so leaving the status as is");
                        return;
                    } else if (payResponse.statusCode / 100 == 2) {
                        billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
                    } else if (payResponse.statusCode / 100 == 4) {
                        logger.error("Rolling back the transaction based on response status code");
                        mfsService.rollbackTransaction(billPayServiceStatus.getMfsTxnid(), COMPANY_CODE + "BILLPAY", (billPayServiceStatus.getCustomerMsisdn() != null ? true : false), billPayServiceStatus);
                    }
                }
            } finally {
                billPayServiceStatusRepo.save(billPayServiceStatus);
            }
        }
    }

    private String getValue(String amountOrTransaction, int index) {
        String[] value = amountOrTransaction.split(",");
        return value[index];
    }
}