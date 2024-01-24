package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.Bill;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.DueBills;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nesco.BillInfoResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nesco.BillPaymentResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nesco.BillInfoDetail;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nesco.PendingBillResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.DisputeTransactionRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.NescoService.NescoPaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillFetcher;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.marshal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NescoService implements BillFetcher<PendingBillResponse>, BillPayer<NescoPaymentState, BillPaymentResponse> {
    protected static final Logger logger = LoggerFactory.getLogger(NescoService.class);

    public class NescoPaymentState extends PaymentState {
        public String transactionId;
        public BillInfoDetail billdata;
    }

    @Bean({"NWZPD_Bill_Fetcher", "NWZPD_Bill_Payer"})
    public NescoService alias() {
        return this;
    }

    @Value("${nesco.request.timeout}")
    int timeout;

    @Value("${nesco.use.proxy}")
    boolean isProxyRequired;

    @Value("${nesco.webservice.url.base}")
    String apiUrlBase;

    @Value("${nesco.auth.key}")
    String apiKey;

    @Value("${nesco.stk.code}")
    String stkCode;

    @Value("${nesco.utility.auth.key}")
    String utilityAuthKey;

    @Value("${nesco.utility.secret.key}")
    String utilitySecretKey;

    @Autowired
    MFSService mfsService;

    @Autowired
    DisputeService disputeService;

    @Autowired
    DisputeTransactionRepository disputeTransactionRepository;

    @Autowired
    PrepaidBillTokenRepository prepaidBillTokenRepository;

    Map<String, String> webserviceHeaders;

    String companyCode = "NWZPD";

    @PostConstruct
    public void afterPropertiesSet() {
        webserviceHeaders = new LinkedHashMap<>() {
            {
                put("Content-Type", "application/json");
                put("AUTH-KEY", apiKey);
                put("STK-CODE", stkCode);
            }
        };
    }

    @Override
    public String getCategory() {
        return "ELEC POST";
    }

    @Override
    public NescoPaymentState getState() {
        return new NescoPaymentState();
    }

    public PendingBillResponse fetchDueBills(String accountNo, Map map) {
        return executeApi(new HashMap() {{
            put("account_no", accountNo);
        }}, "due-bills", PendingBillResponse.class);
    }

    private <T> T executeApi(Map requestPayloadContent, String requestPath, Class<T> clazz) {
        try {
            requestPayloadContent.put("utility_auth_key", utilityAuthKey);
            requestPayloadContent.put("utility_secret_key", utilitySecretKey);

            HttpClient client = getHttpClient();
            String nescoUrl = apiUrlBase + "/" + requestPath;

            return client.postForEntity(nescoUrl, requestPayloadContent, webserviceHeaders, clazz);
        } catch (Exception ex) {
            logger.error("Error in service calling", ex);
            return null;
        }
    }

    @Override
    public DueBills convertToGeneric(PendingBillResponse pendingBills, String accountNo, String msisdn, WalletType wallet_type, Channel channel, Map map) {
        DueBills bills = new DueBills();
        bills.company = companyCode;
        bills.consumerId = accountNo;
        if (pendingBills == null) {
            return null;
        }
        if(pendingBills.getStatus_code() == 001) {
            return bills;
        }
        if(pendingBills.getStatus_code() != 000) {
            return null;
        }
        pendingBills.getData().forEach(x -> {
            Bill bill = new Bill();
            bill.amount = x.getTotal_amount();
            bill.billDueDate = x.getDue_date();
            bill.billIssueDate = x.getIssue_date();
            bill.billMonthYear = x.getMonth() + "-" + x.getYear();
            bill.billNo = x.getBill_number();
            bill.vat = (double) x.getVat_amount();
            bill.serviceCharge = mfsService.getServiceCharge(msisdn, companyCode, x.getTotal_amount(), wallet_type, channel);
            bills.bills.add(bill);
        });
        return bills;
    }

    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient(timeout);
        if (isProxyRequired) {
            client.setDefaultProxy();
        }
        return client;
    }

    @Override
    public void validateRequest(NescoPaymentState paymentState, PaymentRequest request) throws ValidationException {
        if(!request.bill.matches("\\d+")) {
            throw new ValidationException("Bill no format is invalid");
        }
    }

    private BillInfoResponse getNescoBillDetail(String invoiceNo) {
        String transaction_id = StringUtil.generateUniqueReference("9");
        return executeApi(new HashMap<String, String>() {{
            put("transaction_id", transaction_id);
            put("billno", invoiceNo);
        }}, "bill-info", BillInfoResponse.class);
    }

    @Override
    public void preparePayment(NescoPaymentState paymentState, PaymentRequest request) throws HttpErrorResponseException {
        BillInfoResponse billInfoResponse = getNescoBillDetail(request.bill);
        if(billInfoResponse == null || billInfoResponse.getStatus_code() != 0) {
            throw new HttpErrorResponseException(405, billInfoResponse == null ? "No data found" : billInfoResponse.getMessage());
        }
        if(request.amount != billInfoResponse.getData().getTotal_amount()) {
            throw new HttpErrorResponseException(405, "Requested amount is not same as bill amount");
        }
        paymentState.transactionId = billInfoResponse.getTransaction_id();
        paymentState.billdata = billInfoResponse.getData();
    }

    @Override
    public BillPaymentResponse pay(NescoPaymentState paymentState, PaymentRequest request, String mfsChannel) {
        return executeApi(new HashMap<String, Object>() {{
            put("transaction_id", paymentState.transactionId);
            put("core_transaction_id", paymentState.mfsPaymentResponse.txnid);
            put("is_stamp_collected", 0);
        }}, "bill-payment", BillPaymentResponse.class);
    }

    @Override
    public PaymentResult convertToGeneric(NescoPaymentState paymentState, BillPaymentResponse paymentResponse, PaymentRequest request) {
        Integer statusCode = null;
        if (paymentResponse != null) {
            statusCode = paymentResponse.getStatus_code();
        }
        PaymentResult result = new PaymentResult();
        if (statusCode == null || statusCode == 777 || (statusCode == 1 && "received".equalsIgnoreCase(paymentResponse.getMessage()))) {
            result.txnId = paymentState.mfsPaymentResponse.txnid;
            disputeService.insertDisputeRecord(paymentState.billPayServiceStatus, request.msisdn, request.customer);
            paymentState.billPayServiceStatus.setStatus(result.status = BillPayStatus.DISPUTE);
            billPayServiceStatusRepository.save(paymentState.billPayServiceStatus);
        } else if (statusCode == 111) {
            result.txnId = paymentState.mfsPaymentResponse.txnid;
            storePaymentExtraData(paymentState.billPayServiceStatus, paymentState.billdata);
            paymentState.billPayServiceStatus.setStatus(result.status = BillPayStatus.SUCCESS);
            paymentState.billPayServiceStatus.setThirdPartyTxnStatus(paymentResponse.getStatus());
            paymentState.billPayServiceStatus.setThirdPartyTxnid(paymentResponse.getLid());
            billPayServiceStatusRepository.save(paymentState.billPayServiceStatus);
        } else {
            mfsService.rollbackTransaction(paymentState.billPayServiceStatus.getMfsTxnid(), "NESCOBILLPAY", paymentState.billPayServiceStatus.getTransactionType().equals("RETBILLPAY"), paymentState.billPayServiceStatus);
            result.status = paymentState.billPayServiceStatus.getStatus();
        }
        return result;
    }

    private void storePaymentExtraData(BillPayServiceStatus serviceStatus, BillInfoDetail billInfoDetail) {
        try {
            PrepaidBillToken billToken = new PrepaidBillToken();
            billToken.setBillPayTableId(serviceStatus.getId());
            billToken.setFees(Json.toJson(billInfoDetail));
            prepaidBillTokenRepository.save(billToken);
        } catch (JsonProcessingException e) {
            logger.error("Could not store payment detail", e);
        }
    }
}