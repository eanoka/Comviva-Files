package com.grameenphone.wipro.fmfs.mfs_communicator.service.desco;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.HttpResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.desco.DescoTopUpBreakDownRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.desco.DescoTopUpBreakDownResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.desco.DescoTopUpRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.desco.DescoTopUpResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.DisputeService;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.desco.DescoSmartService.DescoSmartPaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.SmsUtil;
import com.grameenphone.wipro.utility.marshal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class DescoSmartService implements BillPayer<DescoSmartPaymentState, DescoTopUpResponse> {
    protected static final Logger logger = LoggerFactory.getLogger(DescoSmartService.class);
    @Value("${desco_prepaid_api_key}")
    String apiKey;
    @Value("${desco_prepaid_timeout}")
    int timeout;
    @Value("${desco_prepaid_ip}")
    String ipAddress;
    @Value("${desco_prepaid_port}")
    String port;
    @Value("${desco_prepaid_use_proxy}")
    boolean isProxyRequired;
    @Value("${desco_prepaid_top_up_breakdown_api}")
    String descoTopUpBrkDwnApi;
    @Value("${desco_prepaid_top_up_api}")
    String descoTopUpApi;
    @Value("${desco_prepaid_top_up_status_check_api}")
    String descoCheckStatusApi;
    @Autowired
    DisputeService disputeService;
    @Autowired
    PrepaidBillTokenRepository prepaidBillTokenRepository;
    private String descoTopUpBrkDwnUrl;
    private String descoTopUpUrl;

    @Bean({"DSCOP_Kaifa_Bill_Payer"})
    public DescoSmartService alias() {
        return this;
    }

    @PostConstruct
    private void initialize() {
        String url = "https://" + ipAddress + ":" + port;
        descoTopUpBrkDwnUrl = url + "/" + descoTopUpBrkDwnApi;
        descoTopUpUrl = url + "/" + descoTopUpApi;
    }

    @Override
    public DescoSmartPaymentState getState() {
        return new DescoSmartPaymentState();
    }

    @Override
    public String getCategory() {
        return "ELEC PRE";
    }

    @Override
    public void validateRequest(DescoSmartPaymentState descoSmartPaymentState, PaymentRequest request) {
        descoSmartPaymentState.transactionId = request.params.get("transaction_id").toString();
    }

    @Override
    public DescoTopUpResponse pay(DescoSmartPaymentState descoSmartPaymentState, PaymentRequest request, String mfsChannel) throws IOException {
        return payToDesco(request.wallet_type.equals(WalletType.SUB) ? request.msisdn : request.customer, descoSmartPaymentState.mfsPaymentResponse.txnid, descoSmartPaymentState.transactionId);
    }

    @Override
    public PaymentResult convertToGeneric(DescoSmartPaymentState descoSmartPaymentState, DescoTopUpResponse descoTopUpResponse, PaymentRequest request) {
        PaymentResult result = new PaymentResult();
        String thirdPartyTxStatus = null;
        try {
            if (descoTopUpResponse == null) {
                descoTopUpResponse = new DescoTopUpResponse();
                result.txnId = descoSmartPaymentState.mfsPaymentResponse.txnid;
                result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
                disputeService.insertDisputeRecord(descoSmartPaymentState.billPayServiceStatus, request.msisdn, request.customer);
                sendSuccessfulSms(request.getConsumerId(), request.amount, descoTopUpResponse.energyCost, descoTopUpResponse.vat, request.msisdn, request.customer);
                return result;
            } else {
                int responseCode = Integer.valueOf(descoTopUpResponse.responseCode);
                if (responseCode >= 400 && responseCode < 500) {
                    sendFailedSms(request.getConsumerId(), String.valueOf(request.amount), request.msisdn, request.customer);
                    mfsService.rollbackTransaction(descoSmartPaymentState.mfsPaymentResponse.txnid, request.getCompany() + "BILLPAY", request.wallet_type.equals(WalletType.RET), descoSmartPaymentState.billPayServiceStatus);
                    result.status = thirdPartyTxStatus = descoSmartPaymentState.billPayServiceStatus.getStatus();
                    return result;

                } else {
                    result.txnId = descoSmartPaymentState.mfsPaymentResponse.txnid;
                    result.status = thirdPartyTxStatus = BillPayStatus.SUCCESS;
                    sendSuccessfulSms(request.getConsumerId(), request.amount, descoTopUpResponse.energyCost, descoTopUpResponse.vat, request.msisdn, request.customer);
                }
            }
        } finally {
            descoSmartPaymentState.billPayServiceStatus.setThirdPartyTxnid(result.vendorTxnId = descoTopUpResponse.transactionId);
            descoSmartPaymentState.billPayServiceStatus.setThirdPartyTxnStatus(descoTopUpResponse.responseCode);
            descoSmartPaymentState.billPayServiceStatus.setStatus(thirdPartyTxStatus);
            descoSmartPaymentState.billPayServiceStatus.setAttr2(descoTopUpResponse.token);
            billPayServiceStatusRepository.save(descoSmartPaymentState.billPayServiceStatus);
        }
        try {
            PrepaidBillToken prepaidBillToken = preparePrepaidBillToken(descoTopUpResponse, descoSmartPaymentState.billPayServiceStatus.getId());
            prepaidBillTokenRepository.save(prepaidBillToken);
        } catch (Exception e) {
            logger.error("Could not insert token in db" + e.getMessage());
        }
        return result;
    }

    public PrepaidBillToken preparePrepaidBillToken(DescoTopUpResponse response, long billpayTableId) throws JsonProcessingException {
        PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setTokenNo(response.token);
        prepaidBillToken.setMeterNo(response.meterNo);
        prepaidBillToken.setCompanyCode("DSCOP");
        prepaidBillToken.setVendAmnt(String.valueOf(response.amount));
        prepaidBillToken.setEngAmnt(String.valueOf(response.energyCost));
        prepaidBillToken.setBillPayTableId(billpayTableId);

        Map<String, Object> feeMap = new HashMap<>();
        feeMap.put("accountNo", response.accountNo);
        feeMap.put("charges", response.charges);
        feeMap.put("revenue", response.revenue);
        feeMap.put("vat", response.vat);
        String fees = Json.toJson(feeMap);
        prepaidBillToken.setFees(fees);

        return prepaidBillToken;
    }

    public DescoTopUpResponse payToDesco(String msisdn, String mfsTxnId, String descoTxnId) {
        DescoTopUpRequest topUpRequest = new DescoTopUpRequest();
        DescoTopUpResponse topUpResposne;

        topUpRequest.setAgentId("");
        topUpRequest.setApiKey(apiKey);
        topUpRequest.setContactNo(msisdn);
        topUpRequest.setRechargeId(mfsTxnId);
        topUpRequest.setTransactionId(descoTxnId);

        long startTime = System.currentTimeMillis();
        HttpResponse response;
        try {
            response = executeDescoWebService(Json.toJson(topUpRequest), descoTopUpUrl);

        } catch (Exception e) {
            logger.error("Error calling Desco Prepaid payment api: ", e);
            return null;
        }
        logger.debug("Total Time Taken in Desco Prepaid Top up " + (System.currentTimeMillis() - startTime));

        try {
            topUpResposne = Json.fromJson(response.response, DescoTopUpResponse.class);
            topUpResposne.responseCode = response.responseCode;

        } catch (Exception e) {
            logger.error("Json Persing Error: ", e.getMessage());
            topUpResposne = new DescoTopUpResponse();
            topUpResposne.responseCode = response.responseCode;
        }
        if (response.isError) {
            int responseCode = Integer.valueOf(response.responseCode);
            if (responseCode >= 400 && responseCode < 500) {
                topUpResposne.responseCode = response.responseCode;
            } else {
                return null;
            }
        }
        return topUpResposne;
    }

    public HttpResponse topUpBreakDown(String consumerId, double amount) {
        DescoTopUpBreakDownRequest request = new DescoTopUpBreakDownRequest();

        request.meterNo = "";
        request.amount = amount;
        request.apiKey = apiKey;
        request.accountNo = consumerId;

        long startTime = System.currentTimeMillis();
        HttpResponse response = new HttpResponse();
        try {
            response = executeDescoWebService(Json.toJson(request), descoTopUpBrkDwnUrl);
        } catch (Exception e) {
            logger.error("Error in calling Desco TopUp Break down UTL: ", e);
            response.isError = true;
            response.response = "Invalid Response";
            return response;
        }
        long endTime = System.currentTimeMillis() - startTime;
        logger.debug("Total time taken in desco topup breakdown url : " + endTime);

        if (response.isError) {
            return response;
        }
        DescoTopUpBreakDownResponse topUpBreakDownResponse;
        try {
            topUpBreakDownResponse = Json.fromJson(response.response, DescoTopUpBreakDownResponse.class);
        } catch (Exception e) {
            logger.error("UnMarshall error: ", e);
            response.isError = true;
            response.response = "Invalid Response";
            return response;
        }
        response.response = topUpBreakDownResponse.transactionId;
        return response;
    }

    private HttpResponse executeDescoWebService(String jsonBody, String url) throws Exception {
        HttpClient httpClient = new HttpClient(timeout);
        httpClient.setDefaultProxy();
        httpClient.setNoExceptionForError(true);

        String responseText = httpClient.post(url, jsonBody, new HashMap<>() {{
            put("content-type", "application/json");
        }});

        int statusCode = httpClient.getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return new HttpResponse(false, responseText, String.valueOf(statusCode));
        } else {
            return new HttpResponse(true, responseText, String.valueOf(statusCode));
        }
    }

    private StringBuilder buildSuccessfulText(String meterNo, double amount, Double energyCost, double vat) {

        StringBuilder sb = new StringBuilder();

        if (energyCost == null) {
            sb.append("Your DESCO smart prepaid meter recharge request is accepted for Account No: ");
            sb.append(meterNo);
            sb.append(", Amount : ");
            sb.append(amount);
            sb.append(";");
        } else {
            sb.append("Your DESCO smart prepaid meter recharge request is accepted for Meter No: ");
            sb.append(meterNo);
            sb.append(", Amount : ");
            sb.append(amount);
            sb.append(", Energy cost : ");
            sb.append(energyCost);
            sb.append(", VAT : ");
            sb.append(vat);
            sb.append(";");
        }
        return sb;
    }
    
    private StringBuilder buildSuccessfulBanglaText(String meterNo, double amount, Double energyCost, double vat) {
        StringBuilder sb = new StringBuilder();
        if (energyCost == null) {
            //It is resolved from dispute process
        	sb.append("আপনার ডেসকো স্মার্ট প্রিপেইড মিটার রিচার্জের অনুরোধ অ্যাকাউন্ট নম্বরের  ");
            sb.append(meterNo);
            sb.append(", এর জন্য গৃহীত হয়েছে: পরিমাণ: ");
            sb.append(amount);
            sb.append(" মিটার শীঘ্রই রিচার্জ করা হবে। আরও কোন প্রশ্নের জন্য ডেসকো তে  যোগাযোগ করুন ");
        } else {
        	
        	sb.append("আপনার ডেসকো স্মার্ট প্রিপেইড মিটার রিচার্জের অনুরোধ গ্রহণ করা হয়েছ।  মিটার নং: ");
            sb.append(meterNo);
            sb.append(", , পরিমাণ :  ");
            sb.append(amount);
            sb.append(",, এনার্জি খরচ : ");
            sb.append(energyCost);
            sb.append(", ভ্যাট : ");
            sb.append(vat);
            sb.append(" ; মিটার শীঘ্রই রিচার্জ করা হবে। আরও কোন প্রশ্নের জন্য ডেসকো তে যোগাযোগ করুন ");
        }
        return sb;
    }

    private void sendSuccessfulSms(String meterNo, double amount, double energyCost, double vat, String msisdn, String custMsisdn) {
        StringBuilder sb = buildSuccessfulText(meterNo, amount, energyCost, vat);
        sb.append(" Meter will be recharged soon. For any further queries please contact DESCO");
        String banglasms = new String(" মিটার শীঘ্রই রিচার্জ করা হবে। আরও কোন প্রশ্নের জন্য ডেসকো তে  যোগাযোগ করুন ");
        String smsBody = sb.toString();
        StringBuilder bsb =buildSuccessfulBanglaText( meterNo,  amount,  energyCost,  vat);
        bsb.append(banglasms);
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        logger.debug("SMS body:: " + smsBody);
        logger.debug("SMS msisdn::" + smsToSend);

        SmsUtil.sendSms(smsToSend, smsBody,bsb.toString(), true); 
    }

    private void sendFailedSms(String accNo, String amount, String msisdn, String custMsisdn) {
        String smsBody = "Your DESCO smart prepaid meter recharge request of " + amount + " for meter " + accNo + " was unsuccessful.";

        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        logger.debug("SMS body:: " + smsBody);
        logger.debug("SMS msisdn::" + smsToSend);
        
        StringBuffer banglasms=new StringBuffer();
        banglasms.append(" মিটার ");
        banglasms.append(accNo);
        banglasms.append(" এর জন্য আপনার ডেসকো প্রিপেইড টোকেন ");
        banglasms.append(amount);
        banglasms.append(" টাকা প্রদানের অনুরোধ ব্যর্থ হয়েছে।");


        SmsUtil.sendSms(smsToSend, smsBody,banglasms.toString(), true);
    }

    public class DescoSmartPaymentState extends PaymentState {
        public String transactionId;
    }
}