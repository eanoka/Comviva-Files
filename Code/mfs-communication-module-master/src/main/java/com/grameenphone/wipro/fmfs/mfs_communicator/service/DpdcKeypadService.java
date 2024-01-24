package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.DpdcResponseFlag;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationResponse.AmountValidationResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.CustomerInfo;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.DataValidationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.DpdcVendingDetail;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.RechargeRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.RechargeResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.SoapDisputeResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.SoapErrorResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.TransactionStatusRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdc.TransactionStatusResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.AmountValidator;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayDisputeResolver;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.SmsUtil;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.marshal.Xml;

@Service
public class DpdcKeypadService implements BillPayer<PaymentState, PaymentResult>, BillPayDisputeResolver, AmountValidator {
    protected static final Logger logger = LoggerFactory.getLogger(DpdcKeypadService.class);
    private static final String DPDC_REMARKS = "DPDC_PRE_BILLPAY";
    private static final String DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm";

    @Autowired
    DisputeService disputeService;

    @Autowired
    PrepaidBillTokenRepository prepaidBillTokenRepository;

    @Value("${dpdc_prepaid_proxy_required}")
    Boolean isProxyRequired;

    @Value("${dpdc_timeout}")
    Integer timeout;

    @Value("${dpdc_data_validation_url}")
    String dataValidationUrl;

    @Value("${dpdc_recharge_url}")
    String rechargeUrl;

    @Value("${dpdc_transaction_status_url}")
    String checkTransactionUrl;

    String companyCode = "DPDCP";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);

    @Bean({"DPDCP_Bill_Payer", "DPDCP_Amount_Validator", "DPDCP_BillPayDisputeResolverService"})
    public DpdcKeypadService alias() {
        return this;
    }

    @Override
    public void validateRequest(PaymentState state, PaymentRequest request) throws ValidationException {
        if (!request.amount_pre_validated) {
            try {
                validateAmount(request.getConsumerId(), String.valueOf(request.amount));
            } catch (ValidationException e) {
                throw e;
            } catch (Throwable e) {
                logger.error("Unable to validate amount ", e);
                throw new ValidationException("Unable to verify amount for DPDCP.");
            }
        }
    }

    @Override
    public String getCategory() {
        return "ELEC PRE";
    }

    @Override
    public PaymentResult pay(PaymentState state, PaymentRequest request, String mfsChannel) throws IOException {
        RechargeResponse response = null;
        try {
            logger.debug("Calling DPDCP bill pay API.");
            response = payToDpdc(request.getConsumerId(), String.valueOf(request.amount), state.mfsPaymentResponse.txnid);
        } catch (Throwable t) {
            logger.error("Error calling in DPDC Payment Confirmation Api", t);
        }

        PaymentResult result = new PaymentResult();
        DpdcVendingDetail paymentResponse = null;
        String billPayStatus = null;
        try {
            if (response == null || (response.body.response != null && response.body.response.message != null && "MSG001".equals(response.body.response.message.messagecode))) {
                result.txnId = state.mfsPaymentResponse.txnid;
                result.status = billPayStatus = BillPayStatus.DISPUTE;
                result.message = "Your payment has been received. Please contact DPDC COMMERCIAL DIVISION for further confirmation";
                disputeService.insertDisputeRecord(state.billPayServiceStatus, request.msisdn, request.customer);
                sendDisputeSMS(request.getConsumerId(), String.valueOf(request.amount), request.msisdn, request.customer);
                return result;
            } else if ((response.body.response.error == null && response.body.response.vending == null) || response.body.response.error != null) {
                sendFailedSms(request.msisdn, request.customer, request.amount, request.getConsumerId());
                if (response.body.response.error != null && response.body.response.error.errorcode != null) {
                    if (response.body.response.error.errorcode.startsWith("OF")) {
                        result.message = response.body.response.error.errormsg;
                    } else if (response.body.response.error.errorcode.startsWith("0100")) {
                        result.message = response.body.response.error.errormsg;
                    } else if (response.body.response.error.errorcode.equals("090006")) {
                        result.message = response.body.response.error.errormsg;
                    } else {
                        result.message = "Unable to recharge DPDC meter";
                    }
                } else {
                    result.message = "Unable to recharge DPDC meter";
                }

                try {
                    mfsService.rollbackTransaction(state.mfsPaymentResponse.txnid, DPDC_REMARKS, request.wallet_type.equals(WalletType.RET), state.billPayServiceStatus);
                } catch (Throwable h) {
                    logger.error("Unable to reverse the transaction", h);
                    state.billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
                }
                result.status = billPayStatus = state.billPayServiceStatus.getStatus();
                return result;
            } else {
                try {
                    paymentResponse = response.body.response.vending;
                    result.txnId = state.mfsPaymentResponse.txnid;
                    result.status = billPayStatus = BillPayStatus.SUCCESS;
                    PrepaidBillToken prepaidBillToken = null;
                    try {
                        prepaidBillToken = preparePrepaidBillToken(paymentResponse, state.billPayServiceStatus.getId());
                        prepaidBillTokenRepository.save(prepaidBillToken);
                    } catch (Throwable e) {
                        logger.error("could not insert dpdc token in db", e);
                    }

                    if (response.body.response.vending.getOnlineRecharge().equalsIgnoreCase("Recharge Successful")) {
                        result.message = "Meter Recharge Successful.";
                        sendSuccessSms(response.body.response.vending, request.msisdn, request.customer, state.mfsPaymentResponse.txnid);
                    } else {
                        result.message = "Meter Recharge Failed.";
                        sendFailedTokenSms(response.body.response.vending, request.msisdn, request.customer, state.mfsPaymentResponse.txnid);
                    }
                } catch (Exception e) {
                    logger.error("SMS Sending Error:", e);
                }
            }
        } finally {
            state.billPayServiceStatus.setStatus(billPayStatus);
            if (paymentResponse != null) {
                state.billPayServiceStatus.setThirdPartyTxnid(result.vendorTxnId = paymentResponse.getOrderId());
                state.billPayServiceStatus.setAttr2(paymentResponse.getToken());
                state.billPayServiceStatus.setAttr3(paymentResponse.getSequence());
            }
            billPayServiceStatusRepository.save(state.billPayServiceStatus);
        }

        return result;
    }

    private RechargeResponse payToDpdc(String consumerId, String amount, String txnid) throws Exception {
        HttpClient httpClient = new HttpClient(timeout);
        if (isProxyRequired) {
            httpClient.setDefaultProxy();
        }
        httpClient.setNoExceptionForError(true);
        RechargeRequest rechargeRequest = new RechargeRequest();
        rechargeRequest.body.request.setCustomerNo(consumerId).setAmount(amount).setMsgId(txnid);
        String rechargeResponseString = httpClient.post(rechargeUrl, rechargeRequest);
        RechargeResponse response = new RechargeResponse();
        DpdcVendingDetail vendingResponse = null;
        SoapErrorResponse errorResponse = null;
        SoapDisputeResponse dispatchResponse = null;
        
        String api_response = rechargeResponseString.replaceAll("&lt;", "<").replaceAll("&gt;", ">");
        String responseString = "";
        
        if(api_response.contains("<Vending>"))
        {
        	responseString = api_response.substring(api_response.indexOf("<Vending>"), api_response.indexOf("</return>"));
        	vendingResponse = Xml.fromXml(responseString, DpdcVendingDetail.class);
        	response.body.response.vending = vendingResponse;
        }
        else if(api_response.contains("<error>"))
        {
        	responseString = api_response.substring(api_response.indexOf("<error>"), api_response.indexOf("</return>"));
        	errorResponse = Xml.fromXml(responseString, SoapErrorResponse.class);
        	response.body.response.error = errorResponse;
        }
        else if(api_response.contains("<message>"))
        {
        	responseString = api_response.substring(api_response.indexOf("<message>"), api_response.indexOf("</return>"));
        	dispatchResponse = Xml.fromXml(responseString, SoapDisputeResponse.class);
        	response.body.response.message = dispatchResponse;
        }
        return response;
    }

    @Override
    public AmountValidationResult validateAmount(com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationRequest request) {
        DataValidationRequest dataRequest = new DataValidationRequest();
        dataRequest.body.dataValidation.setAmount(String.valueOf(request.amount));
        dataRequest.body.dataValidation.setCardData("");
        dataRequest.body.dataValidation.setCustomerNo(request.consumerId);
        AmountValidationResult result = validateAmount(dataRequest);

        if(result.valid) {
            result.service_charge = mfsService.getServiceCharge(request.msisdn, request.company, request.amount, WalletType.valueOf(request.wallet_type), Channel.valueOf(request.channel));
        }

        return result;
    }

    private AmountValidationResult validateAmount(DataValidationRequest request) {
        AmountValidationResult result = new AmountValidationResult();
        CustomerInfo customerInfo;
        SoapErrorResponse error;
        String response;
        String _return;
        String api_response;

        try {
            HttpClient client = new HttpClient(timeout);
            if (isProxyRequired) {
                client.setDefaultProxy();
            }
            client.setNoExceptionForError(true);
            api_response = client.post(dataValidationUrl, request);

            if (api_response == null) {
                result.valid = false;
                result.message = "Unable to validate account. Please try again after some time.";
                return result;
            }

            _return = api_response.replaceAll("&lt;", "<").replaceAll("&gt;", ">");
            if (_return.contains("<CustomerInfo>")) {
                response = _return.substring(_return.indexOf("<CustomerInfo>"), _return.indexOf("</return>"));
                customerInfo = Xml.fromXml(response, CustomerInfo.class);
            } else if (_return.contains("<error>")) {
                response = _return.substring(_return.indexOf("<error>"), _return.indexOf("</return>"));
                error = Xml.fromXml(response, SoapErrorResponse.class);
                result.valid = false;
                if (error.errorcode.startsWith("OF")) {
                    result.message = error.errormsg;
                } else if (error.errorcode.equals("010040")) {
                    result.message = error.errormsg;
                } else {
                    result.message = "Unable to validate account. Please try again after some time.";
                }
                return result;
            } else {
                result.message = "Unable to validate account. Please try again after some time.";
                return result;
            }
            if (customerInfo.getAmountValidationFlag() == DpdcResponseFlag.SUCCESSFUL.getValue()) {
                result.valid = true;
                result.message = "OK";
            } else {
                result.valid = false;
                result.message = customerInfo.getMessage();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("CustomerNo", customerInfo.getCustomerNo());
            data.put("CustomerName", customerInfo.getCustomerName());
            data.put("MeterNo", customerInfo.getMeterNo());
            data.put("MeterType", customerInfo.getMeterType());
            data.put("TariffProgram", customerInfo.getTariffProgram());
            data.put("Mobile", customerInfo.getMobile());
            data.put("Dues", customerInfo.getDues());
            result.data = data;
            return result;
        } catch (IOException e) {
            logger.error("Exception while calling Data validation API : ", e);
            result.valid = false;
            result.message = "Unable to validate account. Please try again after some time.";
            return result;
        }
    }

    private void validateAmount(String consumerId, String amount) throws Exception {
        DataValidationRequest dataRequest = new DataValidationRequest();
        dataRequest.body.dataValidation.setAmount(amount);
        dataRequest.body.dataValidation.setCardData("");
        dataRequest.body.dataValidation.setCustomerNo(consumerId);
        AmountValidationResult result = validateAmount(dataRequest);
        if (!result.valid) {
            throw new ValidationException(result.message);
        }
    }

    private void sendFailedSms(String msisdn, String custMsisdn, double amount, String consumerId) {
        String smsBody = "Your DPDC Prepaid Meter recharge request of Tk " + amount + " for account no " + consumerId + " was unsuccessful.";
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        String banglasmsBody = " মিটার "+consumerId+" এর জন্য আপনার ডিপিডিসি  প্রিপেইড টোকেন "+amount+" টাকা প্রদানের অনুরোধ ব্যর্থ হয়েছে।";

        SmsUtil.sendSms(smsToSend, smsBody,banglasmsBody, true);
    }

    private void sendDisputeSMS(String consumerId, String amount, String msisdn, String custMsisdn) {
        StringBuilder sb = new StringBuilder();
        StringBuilder bsb = new StringBuilder();

        sb.append("Your request is being processed for DPDC prepaid token of Tk ");
        sb.append(amount);
        sb.append(" for account no ");
        sb.append(consumerId);
        sb.append(" at ");
        sb.append(simpleDateFormat.format(new Date()));
        sb.append(".Please wait for confirmation.");

        String smsBody = sb.toString();
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        
        bsb.append("আপনার মিটার ");
        bsb.append(consumerId);
        bsb.append(" এর জন্য ");
        bsb.append(amount);
        bsb.append(" টাকা ");
        bsb.append(simpleDateFormat.format(new Date()));
        bsb.append(" এ ডিপিডিসি  প্রিপেইড টোকেনের  এর অনুরোধটি প্রক্রিয়া করা হচ্ছে। অনুগ্রহ করে নিশ্চিতকরণের জন্য অপেক্ষা করুন।");

        SmsUtil.sendSms(smsToSend, smsBody,bsb.toString(), true);
    }

    public void sendFailedTokenSms(DpdcVendingDetail response, String msisdn, String custMsisdn, String mfsTxnId) {
        StringBuilder sb = new StringBuilder();
        sb.append("DPDC Prepaid Token(s): ");
        sb.append("\n");
        sb.append(response.getToken().replaceAll("\\|", ",\n"));
        sb.append("\n");
        sb.append("Sequence: ").append(response.getSequence());
        sb.append("\n");
        sb.append("Account No: ").append(response.getCustomerNo());
        sb.append("\n");
        sb.append("Meter No: ").append(response.getMeterNo());
        sb.append("\n");
        sb.append("Customer Name: ").append(response.getCustomerName());
        sb.append("\n");
        if (custMsisdn != null) {
            sb.append("Customer No: ").append(custMsisdn);
            sb.append("\n");
        }
        sb.append("Vending Amount: ").append(response.getPaidAmount()).append(" Tk");
        sb.append("\n");
        sb.append("Energy Cost: ").append(response.getEnergyCost()).append(" Tk");
        sb.append("\n");
        Map<String, String> map = response.getFees().getFee().stream().collect(Collectors.toMap((k) -> k.getFeeName(), (v) -> v.getFeeValue()));
        for (String key : map.keySet()) {
            sb.append(key).append(": ").append(map.get(key)).append(" Tk");
            sb.append("\n");
        }
        sb.append("Arrear Amount: ").append(response.getPaydebt()).append(" Tk");
        sb.append("\n");
        sb.append("TrxID: ").append(mfsTxnId);
        sb.append("\n");
        sb.append("Your meter couldn't be recharged online, please enter the token(s) manually. Thank you for using GPAY.");
        String smsBody = sb.toString();
        
        //Start Bangla SMS Body 
        
        StringBuilder bsb = new StringBuilder();
        bsb.append("DPDC প্রিপেইড টোকেন: ");
        bsb.append("\n");
        bsb.append(response.getToken().replaceAll("\\|", ",\n"));
        bsb.append("\n");
        bsb.append("সিকুয়েন্স: ").append(response.getSequence());
        bsb.append("\n");
        bsb.append("হিসাব নাম্বার: ").append(response.getCustomerNo());
        bsb.append("\n");
        bsb.append("মিটার নম্বর: ").append(response.getMeterNo());
        bsb.append("\n");
        bsb.append("গ্রাহকের নাম: ").append(response.getCustomerName());
        bsb.append("\n");
        if (custMsisdn != null) {
            bsb.append("গ্রাহকের মোবাইল: ").append(custMsisdn);
            bsb.append("\n");
        }
        bsb.append("ভেন্ডিং পরিমাণ: ").append(response.getPaidAmount()).append(" Tk");
        bsb.append("\n");
        bsb.append("এনার্জি খরচ: ").append(response.getEnergyCost()).append(" Tk");
        bsb.append("\n");
        
        for (String key : map.keySet()) {
            bsb.append(key).append(": ").append(map.get(key)).append(" Tk");
            bsb.append("\n");
        }
        bsb.append("বকেয়া পরিমাণ: ").append(response.getPaydebt()).append(" Tk");
        bsb.append("\n");
        bsb.append("TrxID: ").append(mfsTxnId);
        bsb.append("\n");
        bsb.append("আপনার মিটার অনলাইনে রিচার্জ করা যায়নি, অনুগ্রহ করে টোকেনটি ম্যানুয়ালি লিখুন। GPAY ব্যবহার করার জন্য আপনাকে ধন্যবাদ।");
        String banglasmbsbody = bsb.toString();
        //End Bangla SMS Body
        logger.debug("DPDCP SMS body:: " + smsBody);
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        logger.debug("DPDCP SMS msisdn::" + smsToSend);
        SmsUtil.sendSms(smsToSend, smsBody,banglasmbsbody, true); 
    }

    public void sendSuccessSms(DpdcVendingDetail response, String msisdn, String custMsisdn, String mfsTxnId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your DPDC Prepaid meter has been online recharged successfully. Thank you for using GPAY.");
        sb.append("\n");
        sb.append("Account No: ").append(response.getCustomerNo());
        sb.append("\n");
        sb.append("Meter No: ").append(response.getMeterNo());
        sb.append("\n");
        sb.append("Customer Name: ").append(response.getCustomerName());
        sb.append("\n");
        if (custMsisdn != null) {
            sb.append("Customer No: ").append(custMsisdn);
            sb.append("\n");
        }
        sb.append("Vending Amount: ").append(response.getPaidAmount()).append(" Tk");
        sb.append("\n");
        sb.append("Energy Cost: ").append(response.getEnergyCost()).append(" Tk");
        sb.append("\n");
        Map<String, String> map = response.getFees().getFee().stream().collect(Collectors.toMap((k) -> k.getFeeName(), (v) -> v.getFeeValue()));
        for (String key : map.keySet()) {
            sb.append(key).append(": ").append(map.get(key)).append(" Tk");
            sb.append("\n");
        }
        sb.append("Arrear Amount: ").append(response.getPaydebt()).append(" Tk");
        sb.append("\n");
        sb.append("TrxID: ").append(mfsTxnId);
        sb.append("\n");
        String smsBody = sb.toString();
        
        StringBuilder banglaSb = new StringBuilder();
        banglaSb.append("আপনার DPDC প্রিপেইড মিটার অনলাইনে সফলভাবে রিচার্জ করা হয়েছে। GPAY ব্যবহার করার জন্য আপনাকে ধন্যবাদ।");
        banglaSb.append("\n");
        banglaSb.append("হিসাব নাম্বার: ").append(response.getCustomerNo());
        banglaSb.append("\n");
        banglaSb.append("মিটার নম্বর: ").append(response.getMeterNo());
        banglaSb.append("\n");
        banglaSb.append("গ্রাহকের নাম: ").append(response.getCustomerName());
        banglaSb.append("\n");
        if (custMsisdn != null) {
            banglaSb.append("গ্রাহকের মোবাইল: ").append(custMsisdn);
            banglaSb.append("\n");
        }
        banglaSb.append("ভেন্ডিং পরিমাণ: ").append(response.getPaidAmount()).append(" Tk");
        banglaSb.append("\n");
        banglaSb.append("এনার্জি খরচ: ").append(response.getEnergyCost()).append(" Tk");
        banglaSb.append("\n");
        
        for (String key : map.keySet()) {
            banglaSb.append(key).append(": ").append(map.get(key)).append(" Tk");
            banglaSb.append("\n");
        }
        banglaSb.append("বকেয়া পরিমাণ: ").append(response.getPaydebt()).append(" Tk");
        banglaSb.append("\n");
        banglaSb.append("TrxID: ").append(mfsTxnId);
        banglaSb.append("\n");
        String banglaSmsBody = banglaSb.toString();

        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        SmsUtil.sendSms(smsToSend, smsBody, banglaSmsBody,true);
    }

    public PrepaidBillToken preparePrepaidBillToken(DpdcVendingDetail response, long billpayTableId) throws JsonProcessingException {
        PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setTokenNo(response.getToken());
        prepaidBillToken.setSeqNo(response.getSequence());
        prepaidBillToken.setMeterNo(response.getMeterNo());
        prepaidBillToken.setCompanyCode(companyCode);
        prepaidBillToken.setVendAmnt(response.getPaidAmount());
        prepaidBillToken.setEngAmnt(response.getEnergyCost());
        prepaidBillToken.setTotalCost(response.getTotalFee());
        prepaidBillToken.setPenalty(response.getPenalty());
        Map<String, Object> feesMap = new HashMap<>();
        feesMap.put("customerno", response.getCustomerNo());
        feesMap.put("customername", response.getCustomerName());
        feesMap.put("posid", response.getPosId());
        feesMap.put("posbalance", response.getPosBalance());
        feesMap.put("grossamount", response.getGrossAmount());
        feesMap.put("paydebt", response.getPaydebt());
        if (response.getFees() != null && response.getFees().getFee() != null) {
            feesMap.put("fees", response.getFees().getFee());
        }
        prepaidBillToken.setFees(Json.toJson(feesMap));
        prepaidBillToken.setBillPayTableId(billpayTableId);
        return prepaidBillToken;
    }

    @Override
    public void resolveDispute(DisputeTransaction disputeTransaction) {
        BillPayServiceStatus billPayServiceStatus = disputeTransaction.getBillPayServiceStatus();
        TransactionStatusResponse transactionStatusResponse = invokeCheckTransaction(billPayServiceStatus.getMfsTxnid());
        RechargeResponse response = null;

        if (transactionStatusResponse == null)
            return;
        if (transactionStatusResponse.body.response != null) {
            if (transactionStatusResponse.body.response.error != null) {
                if (transactionStatusResponse.body.response.error.errorcode.equals("OF012"))
                    response = dpdcRePayment(billPayServiceStatus.getAccountNo(), billPayServiceStatus.getAmount(), billPayServiceStatus.getMfsTxnid());
                else
                    return;
            } else {
                if (transactionStatusResponse.body.response.ordercancel != null && transactionStatusResponse.body.response.ordercancel.getRechargeStatus().equals("02")) {
                    response = dpdcRePayment(billPayServiceStatus.getAccountNo(), billPayServiceStatus.getAmount(), billPayServiceStatus.getMfsTxnid());
                } else {
                    if (transactionStatusResponse.body.response.vending == null)
                        response = dpdcRePayment(billPayServiceStatus.getAccountNo(), billPayServiceStatus.getAmount(), billPayServiceStatus.getMfsTxnid());
                    else {
                        try {
                            billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
                            billPayServiceStatusRepository.save(billPayServiceStatus);

                            PrepaidBillToken prepaidBillToken = null;
                            try {
                                prepaidBillToken = preparePrepaidBillToken(transactionStatusResponse.body.response.vending, billPayServiceStatus.getId());
                                prepaidBillTokenRepository.save(prepaidBillToken);
                            } catch (Exception e) {
                                logger.error("could not insert dpdc token in db", e);
                            }

                            if (transactionStatusResponse.body.response.vending.getOnlineRecharge().equalsIgnoreCase("Recharge Successful")) {
                                sendSuccessSms(response.body.response.vending, billPayServiceStatus.getMsisdn(), billPayServiceStatus.getCustomerMsisdn(), billPayServiceStatus.getMfsTxnid());
                            } else {
                                sendFailedTokenSms(response.body.response.vending, billPayServiceStatus.getMsisdn(), billPayServiceStatus.getCustomerMsisdn(), billPayServiceStatus.getMfsTxnid());
                            }
                            return;
                        } catch (Exception e) {
                            logger.error("SMS Sending Error:", e);
                        }
                    }
                }
            }
            if (response == null) {
                billPayServiceStatus.setStatus(BillPayStatus.DISPUTE);
                disputeService.insertDisputeRecord(billPayServiceStatus, billPayServiceStatus.getMsisdn(), billPayServiceStatus.getCustomerMsisdn());
                sendDisputeSMS(billPayServiceStatus.getAccountNo(), String.valueOf(billPayServiceStatus.getAmount()), billPayServiceStatus.getMsisdn(), billPayServiceStatus.getCustomerMsisdn());
                return;
            } else if ((response.body.response.error == null && response.body.response.vending == null) || response.body.response.error != null) {
                sendFailedSms(billPayServiceStatus.getMsisdn(), billPayServiceStatus.getCustomerMsisdn(), billPayServiceStatus.getAmount(), billPayServiceStatus.getAccountNo());
            } else {
                try {
                    billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
                    billPayServiceStatusRepository.save(billPayServiceStatus);

                    PrepaidBillToken prepaidBillToken = null;
                    try {
                        prepaidBillToken = preparePrepaidBillToken(response.body.response.vending, billPayServiceStatus.getId());
                        prepaidBillTokenRepository.save(prepaidBillToken);
                    } catch (Throwable e) {
                        logger.error("could not insert dpdc token in db", e);
                    }

                    if (response.body.response.vending.getOnlineRecharge().equalsIgnoreCase("Recharge Successful")) {
                        sendSuccessSms(response.body.response.vending, billPayServiceStatus.getMsisdn(), billPayServiceStatus.getCustomerMsisdn(), billPayServiceStatus.getMfsTxnid());
                    } else {
                        sendFailedTokenSms(response.body.response.vending, billPayServiceStatus.getMsisdn(), billPayServiceStatus.getCustomerMsisdn(), billPayServiceStatus.getMfsTxnid());
                    }
                } catch (Exception e) {
                    logger.error("SMS Sending Error:", e);
                }
                return;
            }
        } else {
            return;
        }
        try {
            mfsService.rollbackTransaction(billPayServiceStatus.getMfsTxnid(), "DPDCP", billPayServiceStatus.getCustomerMsisdn() == null ? false : true, billPayServiceStatus);
        } catch (Throwable h) {
            logger.error("Unable to reverse the transaction", h);
            billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
        }
        billPayServiceStatusRepository.save(billPayServiceStatus);
    }

    private TransactionStatusResponse invokeCheckTransaction(String transactionId) {
        TransactionStatusRequest request = new TransactionStatusRequest();
        request.body.request.setMsgid(transactionId);
        TransactionStatusResponse statusResponse = null;
        try {
            logger.debug("Calling DPDCP Transaction Status API.");
            HttpClient client = new HttpClient(timeout);
            if (isProxyRequired) {
                client.setDefaultProxy();
            }
            statusResponse = client.postForEntity(checkTransactionUrl, request, TransactionStatusResponse.class);
        } catch (Throwable t) {
            logger.error("Error calling in DPDCP Transaction Status Api", t);
        }
        return statusResponse;
    }

    private RechargeResponse dpdcRePayment(String consumerId, Double amount, String txnId) {
        RechargeResponse response = null;
        try {
            logger.debug("Calling DPDCP bill pay API.");
            response = payToDpdc(consumerId, String.valueOf(amount), txnId);
        } catch (Throwable t) {
            logger.error("Error calling in DPDC Payment Confirmation Api", t);
        }
        return response;
    }
}