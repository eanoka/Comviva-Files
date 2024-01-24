package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.ServiceProcessingError;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.Bill;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.DueBills;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdcpostpaid.DpdcPostpaidConstants;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdcpostpaid.DpdcPostpaidGetBillRespose;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdcpostpaid.DpdcPostpaidPayBillRespose;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdcpostpaid.DpdcPostpaidReconcileRespose;
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
import com.grameenphone.wipro.utility.marshal.Json;

@Service
public class DPDCPostpaidService implements BillFetcher, BillPayer<PaymentState, PaymentResult>, BillPayDisputeResolver {
    protected static final Logger logger = LoggerFactory.getLogger(DPDCPostpaidService.class);
    private static final String COMPANY_CODE = "DPDC1";
    private static final String DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm";

    @Value("${dpdcpostpaid.use.proxy}")
    boolean isProxyRequired;

    @Value("${dpdcpostpaid.request.timeout}")
    int timeout;

    @Value("${dpdcpostpaid.webservice.url.base}")
    String apiUrlBase;

    @Value("${dpdcpostpaid.user}")
    String dpdcUser;

    @Value("${dpdcpostpaid.password}")
    String dpdcPassword;

    @Value("${dpdcpostpaid.bankcode}")
    String dpdcBankCode;

    @Autowired
    DisputeService disputeService;

    @Autowired
    PrepaidBillTokenRepository prepaidBillTokenRepository;

    @Autowired
    BillPayServiceStatusRepository billPayServiceStatusRepo;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);

    @Override
    public DueBills fetchDueBills(String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map params) throws ValidationException {
        if (consumerId == null || consumerId.isEmpty()) {
            throw new ValidationException("Invalid Consumer number in param list");
        }
        String paychannelCode = "5";
        String billMonth = LocalDate.now().getYear()+"12";
        StringBuilder sb= new StringBuilder();
        sb.append("USR=" + dpdcUser + "&");
        sb.append("PWD=" + dpdcPassword + "&");
        sb.append("CUSTOMER_NO=" + consumerId + "&");
        sb.append("BILL_MONTH=" + billMonth + "&");
        sb.append("PAY_CHANEL=" + paychannelCode  + "&");
        sb.append("BNK_CODE=" + dpdcBankCode + "&");
        sb.append("BILL_TYPE=" + 1);
        String requestParams = sb.toString();

        List<DpdcPostpaidGetBillRespose> billDataList = null;
        try{
            DpdcPostpaidGetBillRespose[] dpdcPostpaidGetBillRespose = executeDPDCGetWebService(apiUrlBase, "getBillData.php", requestParams, DpdcPostpaidGetBillRespose[].class);
            billDataList = Arrays.asList(dpdcPostpaidGetBillRespose);
        }catch(Exception e) {
            logger.error("Error while while fetching the due bills.", e);
            throw new ValidationException("Unable to collect due bill from DPDC Postpaid.");
        }
        if (billDataList == null || billDataList.isEmpty()) {
            logger.info("Unable to collect due bill from DPDC Postpaid.");
            throw new ValidationException("Unable to collect due bill from DPDC Postpaid.");
        }
        DueBills dueBills = new DueBills();
        dueBills.company = COMPANY_CODE;
        dueBills.consumerId = consumerId;

        DpdcPostpaidGetBillRespose billData = billDataList.get(0);

        if (billData.BILL_STATUS.equals("P") || (billData.BILL_STATUS.equals("N") && billData.TOTAL_BILL_AMOUNT.equals("0"))){
            logger.info("No due bills for the consumer number: " + consumerId);
            throw new ValidationException("No due bills for the consumer number: " + consumerId);
        } else if (billData.BILL_STATUS.equals("N")) {
            Bill dueBill = new Bill();
            dueBill.billNo = billData.BILL_NO;
            try {
                dueBill.billDueDate = new SimpleDateFormat("dd-MMM-yy").parse(billData.LAST_PAY_DATE);
            } catch (ParseException e) {
                logger.error("Unable to parse date.", e);
            }
            dueBill.amount = (int) Double.parseDouble(billData.TOTAL_BILL_AMOUNT);
            dueBill.vat = Double.parseDouble(billData.VAT_AMOUNT);
            dueBill.detail = billData;
            if (dueBill.amount == 0) {
                dueBill.serviceCharge = 0.0;
            } else {
                dueBill.serviceCharge = mfsService.getServiceCharge(msisdn, COMPANY_CODE, dueBill.amount, wallet_type, channel);
            }
            dueBills.bills.add(dueBill);
        } else if (billData.BILL_STATUS.equals("K")) {
            logger.info(DpdcPostpaidConstants.responseMessages.get(billData.BILL_STATUS));
            throw new ValidationException("Invalid Account Number.");
        } else {
            logger.info(DpdcPostpaidConstants.responseMessages.get(billData.BILL_STATUS));
            throw new ValidationException(DpdcPostpaidConstants.responseMessages.get(billData.BILL_STATUS));
        }
        return dueBills;
    }

    private <T> T executeDPDCGetWebService(String url, String endpoint, String params, Class<T> clazz) {
        HttpClient client = getHttpClient();
        String dpdcGetUrl = url + "/" + endpoint + "?" + params;
        try {
            return client.getForEntity(dpdcGetUrl, clazz);
        } catch (IOException e) {
            if(e instanceof MismatchedInputException) {
                logger.error("Error:: ", e);
                String textResponse = "";
                if(client.getTextResponse().charAt(0) == '"' && client.getTextResponse().charAt(client.getTextResponse().length()-1) == '"') {
                    textResponse = client.getTextResponse().replace(client.getTextResponse().charAt(0), ' ');
                    textResponse = textResponse.replace(textResponse.charAt(textResponse.length()-1), ' ');
                }
                throw new RuntimeException(textResponse.trim());
            }
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

    private <T> T executeDPDCPostWebService(String url, String endpoint, String params, Class<T> clazz) throws IOException {
        HttpClient client = getHttpClient();

        url = url + "/" + endpoint + "?" + params;
        try {
            return client.postForEntity(url, "", clazz);
        }catch(MismatchedInputException e) {
            logger.error(client.getTextResponse());
        }
        return null;
    }


    @Bean({"DPDC1_Bill_Fetcher", "DPDC1_Bill_Payer", "DPDC1_BillPayDisputeResolverService"})
    public DPDCPostpaidService alias() {
        return this;
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient(timeout);
        if (isProxyRequired) {
            client.setDefaultProxy();
        }
        return client;
    }

    @Override
    public void resolveDispute(DisputeTransaction disputeTransaction) {
        Date txdnate = disputeTransaction.getCreationDate();
        if (!LocalDate.now().equals(LocalDate.ofInstant(txdnate.toInstant(), ZoneId.systemDefault()))) {
            throw new ServiceProcessingError("Dispute Transaction is not of same calendar date", 400);
        }
        StringBuilder sb= new StringBuilder();
        sb.append("USR=" + dpdcUser + "&");
        sb.append("PWD=" + dpdcPassword + "&");
        sb.append("BNK_TRX_ID=" + disputeTransaction.getBillPayServiceStatus().getMfsTxnid() + "&");
        sb.append("BNK_CODE=" + dpdcBankCode + "&");
        sb.append("BILL_TYPE=" + 1);
        String requestParams = sb.toString();
        BillPayServiceStatus billPayServiceStatus = disputeTransaction.getBillPayServiceStatus();
        DpdcPostpaidReconcileRespose reconcileResponse = null;
        try{
            DpdcPostpaidReconcileRespose[] DpdcPostpaidReconcileResponse = executeDPDCGetWebService(apiUrlBase, "checkBillData.php", requestParams, DpdcPostpaidReconcileRespose[].class);
            if (DpdcPostpaidReconcileResponse != null) {
                reconcileResponse = Arrays.asList(DpdcPostpaidReconcileResponse).get(0);
            }
        }catch(Exception e) {
            logger.error("Error: ", e);
            return;
        }
        if (reconcileResponse == null) {
            logger.error("Unable to get reconciliation response, so leaving the status as is");
            return;
        }
        if ("P".equals(reconcileResponse.BILL_STATUS)){
            billPayServiceStatus.setThirdPartyTxnid(reconcileResponse.BANK_TRX_ID);
            billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
            billPayServiceStatusRepo.save(billPayServiceStatus);
            try {
                PrepaidBillToken prepaidBillToken = preparePrepaidBillToken(reconcileResponse, disputeTransaction.getBillPayServiceStatus().getId(), "0");
                prepaidBillTokenRepository.save(prepaidBillToken);
            } catch (Throwable e) {
                logger.error("could not insert dpdc postpaid token in db", e);
            }
            return;
        } else {
            logger.error("Rolling back the transaction based on response status code");
            mfsService.rollbackTransaction(billPayServiceStatus.getMfsTxnid(), COMPANY_CODE + "BILLPAY", (billPayServiceStatus.getCustomerMsisdn() != null ? true : false), billPayServiceStatus);
        }
    }

    @Override
    public String getCategory() {
        return "ELEC POST";
    }

    public static String generateUniqueBillNo(String billType) {
        StringBuilder sb = new StringBuilder();
        sb.append(billType);
        sb.append(String.format("%02d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
        sb.append(("" + System.currentTimeMillis()).substring(3));
        sb.append(String.format("%03d", System.nanoTime() % 1000));
        return sb.toString();
    }
    @Override
    public PaymentResult pay(PaymentState state, PaymentRequest request, String mfsChannel) throws IOException {

        if (!request.amount_pre_validated) {
            request = fetchDueBills(request);
        }

        String customerName = String.valueOf(request.params.get("CUSTOMER_NAME"));
        String totalDpdcAmount = String.valueOf(request.params.get("TOTAL_DPDC_AMOUNT"));
        String vatAmount = String.valueOf(request.params.get("VAT_AMOUNT"));
        String totalBillAmount = String.valueOf(request.params.get("TOTAL_BILL_AMOUNT"));
        String revenueStamp = request.params.containsKey("REVENUE_STAMP") ? String.valueOf(request.params.get("REVENUE_STAMP")) : "0";
        String paychannelCode = "5";
        String billNo = String.valueOf(request.params.get("BILL_NO"));
        String locationCode = String.valueOf(request.params.get("LOCATION_CODE"));

        StringBuilder sb= new StringBuilder();
        sb.append("USR=" + dpdcUser + "&");
        sb.append("PWD=" + dpdcPassword + "&");
        sb.append("LOCATION_CODE=" + locationCode + "&");
        sb.append("CUSTOMER_NO=" + request.getConsumerId() + "&");
        sb.append("CUSTOMER_NAME=" + URLEncoder.encode(customerName, "UTF-8") + "&");
        sb.append("BILL_NO=" + billNo + "&");
        sb.append("TOTAL_DPDC_AMOUNT=" + totalDpdcAmount + "&");
        sb.append("VAT_AMOUNT=" + vatAmount + "&");
        sb.append("TOTAL_BILL_AMOUNT=" + totalBillAmount + "&");
        sb.append("REVENUE_STAMP=" + revenueStamp + "&");
        sb.append("PAY_CHANEL=" + paychannelCode  + "&");
        sb.append("BNK_CODE=" + dpdcBankCode + "&");
        sb.append("BNK_TRX_ID=" + state.billPayServiceStatus.getMfsTxnid() + "&");
        sb.append("BILL_TYPE=" + 1);

        String requestParams = sb.toString();
        PaymentResult result = new PaymentResult();
        DpdcPostpaidPayBillRespose response = null;
        try{
            response = (Arrays.asList(executeDPDCPostWebService(apiUrlBase, "postBillData.php", requestParams, DpdcPostpaidPayBillRespose[].class))).get(0);
        }catch(Exception e) {
            logger.error("Exception occured while calling DPDC postpaid paybill api.", e);
        }
        finally {
            logger.info("response for payBill: " + response);
        }
        String billPayStatus = null;
        try {
            if (response == null || response.paymentStatus == null) {
                result.txnId = state.mfsPaymentResponse.txnid;
                result.status = billPayStatus = BillPayStatus.DISPUTE;
                result.message = "Your request is being processed. Please wait for confirmation SMS.";
                disputeService.insertDisputeRecord(state.billPayServiceStatus, request.msisdn, request.customer);
                return result;
            }
            if (response.paymentStatus.equals("S")) {

                result.txnId = state.mfsPaymentResponse.txnid;
                result.status = billPayStatus = BillPayStatus.SUCCESS;
                result.message = "Your due bill is paid successfully. You will get payment confirmation SMS within short time.";
                PrepaidBillToken prepaidBillToken = null;
                try {
                    prepaidBillToken = preparePrepaidBillToken(request, state.billPayServiceStatus.getId(), revenueStamp);
                    prepaidBillTokenRepository.save(prepaidBillToken);
                } catch (Throwable e) {
                    logger.error("could not insert dpdc postpaid token in db", e);
                }

                return result;
            }else {
                result.message = DpdcPostpaidConstants.responseMessages.get(response.paymentStatus);
                result.status = billPayStatus = BillPayStatus.FAIL;
                result.txnId = state.mfsPaymentResponse.txnid;
                try {
                    mfsService.rollbackTransaction(state.mfsPaymentResponse.txnid, "DPDC POSTPAID bill payment failed.", request.wallet_type.equals(WalletType.RET), state.billPayServiceStatus);
                } catch (Throwable h) {
                    logger.error("Unable to reverse the transaction", h);
                    state.billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
                }
                return result;
            }
        } finally {
            state.billPayServiceStatus.setStatus(billPayStatus);
            billPayServiceStatusRepository.save(state.billPayServiceStatus);
        }
    }

    private PaymentRequest fetchDueBills(PaymentRequest request) throws ValidationException {
        DueBills dueBills = fetchDueBills(request.getConsumerId(), request.msisdn, request.wallet_type, request.channel, request.params);
        Bill bill = null;
        if(!dueBills.bills.isEmpty()) {
            bill = dueBills.bills.get(0);
        }

        ObjectMapper oMapper = new ObjectMapper();
        Map<String, Object> map = oMapper.convertValue(bill.detail, Map.class);
        request.params = map;
        request.amount = Double.parseDouble(map.get("TOTAL_BILL_AMOUNT").toString());

        request.amount_pre_validated = true;
        request.consumer_pre_validated = true;

        return request;
    }

    private PrepaidBillToken preparePrepaidBillToken(PaymentRequest request, long billPayId, String revenueStamp) throws JsonProcessingException {
        PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setCompanyCode(COMPANY_CODE);
        prepaidBillToken.setBillPayTableId(billPayId);

        Map<String, Object> feeMap = new HashMap<>();
        feeMap.put("customerName", request.params.get("CUSTOMER_NAME"));
        feeMap.put("vat", request.params.get("VAT_AMOUNT"));
        feeMap.put("revenueStamp", revenueStamp);
        feeMap.put("locationCode", request.params.get("LOCATION_CODE"));
        feeMap.put("totalDpdcAmount", request.params.get("TOTAL_DPDC_AMOUNT"));
        String fees = Json.toJson(feeMap);
        prepaidBillToken.setFees(fees);

        return prepaidBillToken;
    }

    private PrepaidBillToken preparePrepaidBillToken(DpdcPostpaidReconcileRespose response, long billPayId, String revenueStamp) throws JsonProcessingException {

        PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setCompanyCode(COMPANY_CODE);
        prepaidBillToken.setBillPayTableId(billPayId);

        Map<String, Object> feeMap = new HashMap<>();
        feeMap.put("customerName", response.CUSTOMER_NAME);
        feeMap.put("vat", response.VAT_AMOUNT);
        feeMap.put("revenueStamp", revenueStamp);
        feeMap.put("locationCode", response.LOCATION_CODE);
        feeMap.put("totalDpdcAmount", response.TOTAL_DPDC_AMOUNT);
        String fees = Json.toJson(feeMap);
        prepaidBillToken.setFees(fees);

        return prepaidBillToken;
    }

    @Override
    public void validateRequest(PaymentState paymentState, PaymentRequest request) throws ValidationException {
        String paychannelCode = "5";
        String billMonth = LocalDate.now().getYear()+"12";
        StringBuilder sb= new StringBuilder();
        sb.append("USR=" + dpdcUser + "&");
        sb.append("PWD=" + dpdcPassword + "&");
        sb.append("CUSTOMER_NO=" + request.getConsumerId() + "&");
        sb.append("BILL_MONTH=" + billMonth + "&");
        sb.append("PAY_CHANEL=" + paychannelCode  + "&");
        sb.append("BNK_CODE=" + dpdcBankCode + "&");
        sb.append("BILL_TYPE=" + 1);
        String requestParams = sb.toString();

        List<DpdcPostpaidGetBillRespose> billDataList = null;
        try{
            DpdcPostpaidGetBillRespose[] dpdcPostpaidGetBillRespose = executeDPDCGetWebService(apiUrlBase, "getBillData.php", requestParams, DpdcPostpaidGetBillRespose[].class);
            billDataList = Arrays.asList(dpdcPostpaidGetBillRespose);
        }catch(Exception e) {
            logger.error("Error while while fetching the due bills.", e);
            throw new ValidationException("Unable to collect due bill from DPDC Postpaid.");
        }
        if(!String.valueOf(Double.parseDouble(billDataList.get(0).TOTAL_BILL_AMOUNT)).equals(String.valueOf(request.amount)))
        {
            throw new ValidationException("Bill amount mismatch.");
        }
    }

}