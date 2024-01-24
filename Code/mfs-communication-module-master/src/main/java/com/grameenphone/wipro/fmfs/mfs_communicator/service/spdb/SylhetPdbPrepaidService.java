package com.grameenphone.wipro.fmfs.mfs_communicator.service.spdb;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.spdb.AmmeterSoap;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.spdb.PayPower;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.spdb.SOAPEnvelopeRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.spdb.SOAPEnvelopeResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.DisputeService;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.spdb.SylhetPdbPrepaidService.SylhetPdbPaymentState;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.SmsUtil;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.security.CryptoUtil;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Service
public class SylhetPdbPrepaidService implements BillPayer<SylhetPdbPaymentState, PayPower> {
    public static final String VALID_RESPONSE_CODE = "00000";
    public static final String REFUND_RESPONSE_CODE = "01002";
    public static final String VALID_METER_STATUS = "Meter Active";
    protected static final Logger logger = LoggerFactory.getLogger(SylhetPdbPrepaidService.class);
    private static final SimpleDateFormat DATE_TIME_PATTERN = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    @Value("${pdbsp_enel_id}")
    String ENEL_ID;
    @Value("${pdbsp_cust_id}")
    String CUST_ID;
    @Value("${pdbsp_enc_key}")
    String ENC_KEY;
    @Value("${pdbsp_user_id}")
    String USER_ID;
    @Value("${pdbsp_service_url}")
    String serviceUrl;
    @Value("${pdbsp_use_proxy}")
    boolean useProxy;
    @Value("${pdbsp_api_timeout}")
    int timeout;
    @Autowired
    DisputeService disputeService;
    @Autowired
    PrepaidBillTokenRepository prepaidBillTokenRepository;
    String companyCode = "PDBSP";

    @Bean({"PDBSP_Bill_Payer"})
    public SylhetPdbPrepaidService alias() {
        return this;
    }

    @Override
    public String getCategory() {
        return "ELEC PRE";
    }

    @Override
    public SylhetPdbPaymentState getState() {
        return new SylhetPdbPaymentState();
    }

    @Override
    public void validateRequest(SylhetPdbPaymentState sylhetPdbPaymentState, PaymentRequest request) throws ValidationException {
        try {
            sylhetPdbPaymentState.sessionId = getSessionId(request.getConsumerId());
        } catch (Throwable h) {
            logger.error("Unable to collect session id", h);
            throw new ValidationException("Invalid Meter");
        }

        try {
            validateAmount(sylhetPdbPaymentState.sessionId, request.getConsumerId(), request.amount);
        } catch (Throwable h) {
            logger.error("Unable to validate amount ", h);
            throw new ValidationException("Invalid Amount");
        }
    }

    @Override
    public PayPower pay(SylhetPdbPaymentState sylhetPdbPaymentState, PaymentRequest request, String mfsChannel) throws IOException {
        PayPower power = null;
        try {
            power = payToPDBSP(sylhetPdbPaymentState.sessionId, request.getConsumerId(), request.amount);
        } catch (Throwable e) {
            logger.debug("Couldn't pay bill for " + request.getConsumerId(), e);
        }
        return power;
    }

    private PayPower payToPDBSP(String sessionId, String meterNo, double amount) throws IOException, NoSuchAlgorithmException {
        PayPower soapData = preparePayPowerSoapData(sessionId, meterNo, amount);
        logger.debug("Calling PDBSP bill pay API.");
        return callApi("IPayPower", soapData);
    }

    @Override
    public PaymentResult convertToGeneric(SylhetPdbPaymentState sylhetPdbPaymentState, PayPower payPower, PaymentRequest request) {
        PaymentResult result = new PaymentResult();
        String thirdPartyTxStatus = null;
        try {
            if (payPower == null) {
                payPower = new PayPower();
                sendDisputeSMS(request.getConsumerId(), String.valueOf(request.amount), request.msisdn, request.customer);
                result.txnId = sylhetPdbPaymentState.mfsPaymentResponse.txnid;
                result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
                disputeService.insertDisputeRecord(sylhetPdbPaymentState.billPayServiceStatus, request.msisdn, request.customer);
                return result;
            } else if (REFUND_RESPONSE_CODE.equals(payPower.responseCode)) {
                sendFailedSms(request.getConsumerId(), String.valueOf(request.amount), request.msisdn, request.customer);
                try {
                mfsService.rollbackTransaction(sylhetPdbPaymentState.mfsPaymentResponse.txnid, "PDBSPBILLPAY", request.wallet_type.equals(WalletType.RET), sylhetPdbPaymentState.billPayServiceStatus);
                } catch (Throwable h) {
                    logger.error("Unable to reverse the transaction", h);
                    sylhetPdbPaymentState.billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
                }
                result.status = thirdPartyTxStatus = sylhetPdbPaymentState.billPayServiceStatus.getStatus();
                return result;
            } else if (VALID_RESPONSE_CODE.equals(payPower.bodySection.responseCode)) {
                sendSuccessSms(payPower, request.msisdn, request.customer, sylhetPdbPaymentState.mfsPaymentResponse.txnid);
                result.txnId = sylhetPdbPaymentState.mfsPaymentResponse.txnid;
                result.status = thirdPartyTxStatus = BillPayStatus.SUCCESS;
            } else {
                sendDisputeSMS(request.getConsumerId(), String.valueOf(request.amount), request.msisdn, request.customer);
                result.txnId = sylhetPdbPaymentState.mfsPaymentResponse.txnid;
                result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
                disputeService.insertDisputeRecord(sylhetPdbPaymentState.billPayServiceStatus, request.msisdn, request.customer);
                return result;
            }
        } finally {
            sylhetPdbPaymentState.billPayServiceStatus.setThirdPartyTxnid(result.vendorTxnId = payPower.bodySection.trxNo);
            sylhetPdbPaymentState.billPayServiceStatus.setThirdPartyTxnStatus(payPower.bodySection.responseCode == null ? payPower.responseCode : payPower.bodySection.responseCode);
            sylhetPdbPaymentState.billPayServiceStatus.setStatus(thirdPartyTxStatus);
            sylhetPdbPaymentState.billPayServiceStatus.setAttr2(payPower.bodySection.token);
            billPayServiceStatusRepository.save(sylhetPdbPaymentState.billPayServiceStatus);
        }
        try {
            PrepaidBillToken prepaidBillToken = preparePrepaidBillToken(payPower, sylhetPdbPaymentState.billPayServiceStatus.getId());
            prepaidBillTokenRepository.save(prepaidBillToken);
        } catch (Exception e) {
            logger.debug("Unable to save token in db " + e.getMessage());
        }
        return result;
    }

    public PrepaidBillToken preparePrepaidBillToken(PayPower response, long billPayServiceStatusId) throws Exception {
        PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setTokenNo(response.bodySection.token);
        prepaidBillToken.setMeterNo(response.bodySection.meter);
        prepaidBillToken.setCompanyCode(companyCode);
        prepaidBillToken.setVendAmnt(response.bodySection.amount.toString());
        prepaidBillToken.setEngAmnt(response.bodySection.energyCost.toString());
        prepaidBillToken.setTotalCost(response.bodySection.fee.toString());
        prepaidBillToken.setBillPayTableId(billPayServiceStatusId);
        Map<String, Object> feeMap = new HashMap<>();
        feeMap.put("ACCOUNT", response.bodySection.account);
        feeMap.put("RESOURCE_TYPE", response.bodySection.resourceType);
        feeMap.put("VAT", response.bodySection.vat);
        feeMap.put("ENERGY", response.bodySection.energy);
        feeMap.put("ACCOUNTSAVEAMOUNT", response.bodySection.savedAmount);
        feeMap.put("ACCOUNTPAYAMOUNT", response.bodySection.payAmount);
        feeMap.put("CCY", response.bodySection.currency);
        feeMap.put("CASH_AC_BAL", response.bodySection.acBalance);
        feeMap.put("VAT_DETAIL", response.bodySection.VAT_DETAIL);
        prepaidBillToken.setFees(Json.toJson(feeMap));
        return prepaidBillToken;
    }

    public void validateAmount(String sessionId, String meterNo, double amount) throws IOException, NoSuchAlgorithmException {
        logger.debug("Validating PDBSP amount.");
        PayPower soapData = preparePayPowerSoapData(sessionId, meterNo, amount);
        PayPower response = callApi("ITrialPayPower", soapData);
        if (!VALID_RESPONSE_CODE.equals(response.bodySection.responseCode)) {
            throw new ValidationException(response.message == null ? response.bodySection.message : response.message);
        }
    }

    private PayPower preparePayPowerSoapData(String sessionId, String meterNo, double amount) throws NoSuchAlgorithmException {
        PayPower soapData = new PayPower();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formatDateStr = formatter.format(soapData.topSection.requestTime);
        TreeMap<String, String> paramMap = new TreeMap<>();
        String formattedAmount = String.format("%.2f", amount);
        paramMap.put("VERSION", "1.0");
        paramMap.put("SOURCE", "0");
        paramMap.put("CUST_ID", CUST_ID);
        paramMap.put("REQUEST_TIME", formatDateStr);
        paramMap.put("ENEL_ID", ENEL_ID);
        paramMap.put("METER_NO", meterNo);
        paramMap.put("SIGN_TYPE", "1");
        paramMap.put("POWER_AMT", formattedAmount);
        paramMap.put("SESSION_ID", sessionId);
        paramMap.put("USER_ID", USER_ID);
        String payload = HttpClient.serializeMap(paramMap, false) + "&KEY=" + ENC_KEY;
        logger.debug("Payload for IPayPower API: " + payload);
        String sign = CryptoUtil.hash("MD5", payload);
        soapData.topSection.custId = CUST_ID;
        soapData.bodySection.enel = ENEL_ID;
        soapData.bodySection.meter = meterNo;
        soapData.bodySection.session = sessionId;
        soapData.bodySection.powerAmount = formattedAmount;
        soapData.bodySection.user = USER_ID;
        soapData.tailSection.signature = sign;
        return soapData;
    }

    private String getSessionId(String meterNo) throws IOException, NoSuchAlgorithmException {
        logger.debug("Getting session Id from PDBSP.");
        AmmeterSoap soapData = new AmmeterSoap();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formatDateStr = formatter.format(soapData.topSection.requestTime);
        TreeMap<String, String> paramMap = new TreeMap<>();
        paramMap.put("VERSION", "1.0");
        paramMap.put("SOURCE", "0");
        paramMap.put("CUST_ID", CUST_ID);
        paramMap.put("REQUEST_TIME", formatDateStr);
        paramMap.put("ENEL_ID", ENEL_ID);
        paramMap.put("METER_NO", meterNo);
        paramMap.put("SIGN_TYPE", "1");
        String payload = HttpClient.serializeMap(paramMap, false) + "&KEY=" + ENC_KEY;
        logger.debug("Payload for IQAmmeter API: " + payload);
        String sign = CryptoUtil.hash("MD5", payload);
        soapData.topSection.custId = CUST_ID;
        soapData.bodySection.enel = ENEL_ID;
        soapData.bodySection.meter = meterNo;
        soapData.tailSection.signature = sign;
        AmmeterSoap response = callApi("IQAmmeter", soapData);
        logger.debug("Session from IQAmmeter API: " + response.bodySection.session);
        if (VALID_RESPONSE_CODE.equals(response.bodySection.responseCode)) {
            if (!VALID_METER_STATUS.equals(response.bodySection.status)) {
                throw new ValidationException("Meter is not active");
            }
            return response.bodySection.session;
        }
        throw new ValidationException(response.message == null ? response.bodySection.status : response.message);
    }

    private <T> T callApi(String method, T reqEntity) throws IOException {
        logger.debug("Request for " + method + " API");
        HttpClient httpClient = new HttpClient(timeout);
        if (useProxy) {
            httpClient.setDefaultProxy();
        }
        SOAPEnvelopeRequest request = new SOAPEnvelopeRequest();
        request.body.trans.setArg0(method);
        request.body.trans.setArg1(reqEntity);
        SOAPEnvelopeResponse response = httpClient.postForEntity(serviceUrl, request, SOAPEnvelopeResponse.class);
        if (response == null) {
            return null;
        }
        return response.getReturn((Class<T>) reqEntity.getClass());
    }

    protected String prepareNotificationTextForSMS(PayPower response, String custMsisdn, String mfsTxnId) {
        StringBuilder sb = new StringBuilder();
        sb.append("BPDB Sylhet Metro Prepaid Token(s):");
        sb.append("\n");
        sb.append(response.bodySection.token.replaceAll("\\|", ",\n"));
        sb.append("\n");
        sb.append("Meter No: ").append(response.bodySection.meter);
        sb.append("\n");
        if (custMsisdn != null) {
            sb.append("Customer No: ").append(custMsisdn);
            sb.append("\n");
        }
        sb.append("Vending Amount: ").append(response.bodySection.amount).append(" Tk");
        sb.append("\n");
        sb.append("Energy Cost: ").append(response.bodySection.energyCost).append(" Tk");
        sb.append("\n");
        sb.append("Fee: ").append(response.bodySection.fee).append(" Tk");
        sb.append("\n");
        sb.append("Total Charge: ").append(response.bodySection.vat).append(" Tk");
        sb.append("\n");
        if(response.bodySection.VAT_DETAIL != null) {
            response.bodySection.VAT_DETAIL.forEach(detail -> {
                sb.append(detail.ITEM_NAME).append(": ").append(detail.ITEM_VALUE).append(" Tk");
                sb.append("\n");
            });
        }
        sb.append("Energy: ").append(response.bodySection.energy);
        sb.append("\n");
        sb.append("TrxID: ").append(mfsTxnId);
        return sb.toString();
    }
    
    protected String prepareBanglaNotificationText(PayPower response, String custMsisdn, String mfsTxnId) {
		StringBuilder sb = new StringBuilder();
		
		sb.append(" পিডিবি সিলেট মেট্রো প্রিপেইড টোকেন: ");
		sb.append("\n");
		sb.append(response.bodySection.token.replaceAll("\\|" , ",\n"));
		sb.append("\n");
		sb.append("মিটার নম্বর: ").append(response.bodySection.meter);
		sb.append("\n");
		if (custMsisdn!=null) {
			sb.append("গ্রাহকের মোবাইল: ").append(custMsisdn);
			sb.append("\n");
		}
		sb.append("ভেন্ডিং পরিমাণ: ").append(response.bodySection.amount).append(" Tk");
		sb.append("\n");
		sb.append("এনার্জি খরচ: ").append(response.bodySection.energyCost).append(" Tk");
		sb.append("\n");
		sb.append(" ফি: ").append(response.bodySection.fee).append(" Tk");
		sb.append("\n");
		sb.append(" সর্বমোট চার্জ: ").append(response.bodySection.vat).append(" Tk");
		sb.append("\n");
		if(response.bodySection.VAT_DETAIL != null) {
			response.bodySection.VAT_DETAIL.forEach(detail -> {
				sb.append(detail.ITEM_NAME).append(": ").append(detail.ITEM_VALUE).append(" Tk");
				sb.append("\n");
			});
		}
		sb.append("এনার্জি:  ").append(response.bodySection.energy);
		sb.append("\n");
		sb.append("TrxID: ").append(mfsTxnId);
		return sb.toString();
	}

    public void sendSuccessSms(PayPower response, String msisdn, String custMsisdn, String mfsTxnId) {
        String smsBody = prepareNotificationTextForSMS(response, custMsisdn, mfsTxnId);
        String banglaSmsBody=prepareBanglaNotificationText(response, custMsisdn, mfsTxnId);
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        SmsUtil.sendSms(smsToSend, smsBody,banglaSmsBody, true); 
    }

    public void sendDisputeSMS(String accountNo, String amount, String msisdn, String custMsisdn) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your request is being processed for Sylhet PDB prepaid token of Tk ");
        sb.append(amount);
        sb.append(" for meter ");
        sb.append(accountNo);
        sb.append(" at ");
        sb.append(DATE_TIME_PATTERN.format(new Date()));
        sb.append(". Please wait for confirmation.");
        String smsBody = sb.toString();
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        
        StringBuilder bsb = new StringBuilder();
		bsb.append("সিলেট পিডিবি প্রিপেইড টোকেনের জন্য আপনার অনুরোধটি ");
		bsb.append(accountNo);
		bsb.append(" মিটার এর জন্য ");
		bsb.append(DATE_TIME_PATTERN.format(new Date()));
		bsb.append(" এ ");
		bsb.append(amount);
		bsb.append(" টাকা প্রক্রিয়া করা হচ্ছে। নিশ্চিতকরণের জন্য অপেক্ষা করুন।");
		
        SmsUtil.sendSms(smsToSend, smsBody,bsb.toString(), true);
    }

    public void sendFailedSms(String accountNo, String amount, String msisdn, String custMsisdn) {
        String smsBody = "Your Sylhet PDB prepaid token request of Tk " + amount + " for meter " + accountNo + " was unsuccessful.";
        String banglasmsbody="মিটার "+accountNo+" এর জন্য আপনার সিলেট পিডিবি প্রিপেইড টোকেন "+amount+" টাকা এর অনুরোধ ব্যর্থ হয়েছে।";
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        SmsUtil.sendSms(smsToSend, smsBody,banglasmsbody, true);
    }

    public class SylhetPdbPaymentState extends PaymentState {
        public String sessionId;
    }
}