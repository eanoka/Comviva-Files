package com.grameenphone.wipro.fmfs.mfs_communicator.service.desco;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationResponse.AmountValidationResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ConsumerValidateRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ConsumerValidationResponse.ConsumerValidationResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.desco.unified_meter.DescoUnifiedRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.desco.unified_meter.DescoUnifiedResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.DisputeService;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.desco.DescoUnifiedMeterService.DescoUnifiedPaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.AmountValidator;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayDisputeResolver;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.ConsumerValidator;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.SmsUtil;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.marshal.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class DescoUnifiedMeterService implements BillPayer<DescoUnifiedPaymentState, DescoUnifiedResponse>, ConsumerValidator, AmountValidator, BillPayDisputeResolver {
    protected static final Logger logger = LoggerFactory.getLogger(DescoUnifiedMeterService.class);
    private static final int[] descoResponseCodeList = {404, 414, 415, 416, 417, 418, 428, 429};

    @Value("${desco_unified_account_validation_url}")
    String accountValidationUrl;
    @Value("${desco_unified_amount_validation_url}")
    String amountValidationUrl;
    @Value("${desco_payment_url}")
    String paymentUrl;
    @Value("${desco_ack_url}")
    String ackUrl;
    @Value("${desco_tnx_status_url}")
    String txnStatusUrl;
    @Value("${desco_unified_timeout}")
    int timeout;
    @Value("${desco_unified_use_proxy}")
    boolean useProxy;
    @Value("${desco_unified_user_name}")
    String userName;
    @Value("${desco_unified_password}")
    String password;
    @Value("${desco_unified_api_key}")
    String apiKey;

    @Autowired
    DisputeService disputeService;

    @Autowired
    PrepaidBillTokenRepository prepaidBillTokenRepository;

    @Bean({"DSCOP_Unified_Bill_Payer", "DSCOP_Consumer_Validator", "DSCOP_Amount_Validator", "DSCOP_BillPayDisputeResolverService"})
    public DescoUnifiedMeterService alias() {
        return this;
    }

    @Override
    public DescoUnifiedPaymentState getState() {
        return new DescoUnifiedPaymentState();
    }

    @Override
    public String getCategory() {
        return "ELEC PRE";
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
    public void validateRequest(DescoUnifiedPaymentState state, PaymentRequest request) throws ValidationException {
        if (!request.consumer_pre_validated) {
            DescoUnifiedResponse descoUnifiedResponse = validateAccount(request.getConsumerId());
            if (descoUnifiedResponse == null) {
                throw new ValidationException("Unable to validate account number");
            }
            if (descoUnifiedResponse.state == 200) {
                state.transactionId = descoUnifiedResponse.transId;
            } else if (Arrays.stream(descoResponseCodeList).anyMatch(i -> i == descoUnifiedResponse.state)) {
                throw new ValidationException(descoUnifiedResponse.message);
            } else {
                throw new ValidationException("Invalid Account Number");
            }
        } else {
            state.transactionId = (String) request.params.get("transaction_id");
        }

        if (!request.amount_pre_validated) {
            String validateAmountResponse = validateAmount(request.getConsumerId(), state.transactionId, String.valueOf(request.amount), request.customer);
            if (validateAmountResponse != null) {
                throw new ValidationException(validateAmountResponse);
            }
        }
    }

    @Override
    public DescoUnifiedResponse pay(DescoUnifiedPaymentState descoUnifiedPaymentState, PaymentRequest request, String mfsChannel) throws IOException {
        return payToDesco(request.getConsumerId(), descoUnifiedPaymentState.transactionId, String.valueOf(request.amount), request.customer);
    }

    @Override
    public PaymentResult convertToGeneric(DescoUnifiedPaymentState state, DescoUnifiedResponse paymentResponse, PaymentRequest request) {
        PaymentResult result = new PaymentResult();
        String thirdPartyTxStatus = null;

        try {
            if (paymentResponse == null) {
                paymentResponse = new DescoUnifiedResponse();
                sendDisputeSms(paymentResponse, request.getConsumerId(), request.amount, request.msisdn, request.wallet_type.equals(WalletType.RET) ? request.customer : null);
                result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
                result.txnId = state.mfsPaymentResponse.txnid;
                disputeService.insertDisputeRecord(state.billPayServiceStatus, request.msisdn, request.customer);
                result.message = "Your payment has been received & request is being processed. Please wait for confirmation SMS.";
                return result;
            } else if (paymentResponse.state == 200) {
                DescoUnifiedResponse ackResponse = null;
                try {
                    ackResponse = collectAcknowledgement(request.getConsumerId(), state.transactionId, String.valueOf(request.amount));
                } catch (Throwable h) {
                    logger.error("Error occurred while calling acknowledge API.", h);
                }
                if (ackResponse != null && ackResponse.state == 200) {
                    result.txnId = state.mfsPaymentResponse.txnid;
                    result.status = thirdPartyTxStatus = BillPayStatus.SUCCESS;
                    result.message = prepareSuccessMessage(paymentResponse);
                    sendSuccessfulSms(paymentResponse, request.msisdn, request.wallet_type.equals(WalletType.RET) ? request.customer : null, state.mfsPaymentResponse.txnid);
                } else {
                    sendDisputeSms(paymentResponse, request.getConsumerId(), request.amount, request.msisdn, request.wallet_type.equals(WalletType.RET) ? request.customer : null);
                    result.txnId = state.mfsPaymentResponse.txnid;
                    result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
                    result.message = "Your payment has been received & request is being processed. Please wait for confirmation SMS.";
                    disputeService.insertDisputeRecord(state.billPayServiceStatus, request.msisdn, request.customer);
                    return result;
                }
            } else {
                sendFailedSms(request.getConsumerId(), request.amount, request.msisdn, request.wallet_type.equals(WalletType.RET) ? request.customer : null);
                mfsService.rollbackTransaction(state.mfsPaymentResponse.txnid, request.getCompany() + "BILLPAY", request.wallet_type.equals(WalletType.RET), state.billPayServiceStatus);
                final int descoResponseState = paymentResponse.state;
                if (Arrays.stream(descoResponseCodeList).anyMatch(i -> i == descoResponseState)) {
                    result.message = paymentResponse.message;
                } else {
                    result.message = "We cannot process you request at this time, please try again later.";
                }
                result.status = thirdPartyTxStatus = state.billPayServiceStatus.getStatus();
                return result;
            }
        } finally {
            state.billPayServiceStatus.setThirdPartyTxnid(result.vendorTxnId = state.transactionId);
            state.billPayServiceStatus.setThirdPartyTxnStatus(String.valueOf(paymentResponse.state));
            state.billPayServiceStatus.setStatus(thirdPartyTxStatus);
            state.billPayServiceStatus.setAttr1("U");
            state.billPayServiceStatus.setAttr2(paymentResponse.token);
            state.billPayServiceStatus.setAttr3(paymentResponse.seq);
            billPayServiceStatusRepository.save(state.billPayServiceStatus);
        }
        try {
            PrepaidBillToken prepaidBillToken = preparePrepaidBillToken(paymentResponse, request.getCompany(), (int) state.billPayServiceStatus.getId(), request.wallet_type.equals(WalletType.RET) ? request.customer : null, state.mfsPaymentResponse.txnid);
            prepaidBillTokenRepository.save(prepaidBillToken);
        } catch (Exception e) {
            logger.error("Could not insert token in db:: ", e);
        }
        return result;
    }

    private String prepareSuccessMessage(DescoUnifiedResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your bill paid successful. ");
        sb.append("Your DESCO Prepaid Token is ");
        sb.append(response.token);
        sb.append(", Seq No:");
        sb.append(response.seq);
        sb.append(",for Meter No:");
        sb.append(response.meterNo);
        sb.append(",Vending Amt:");
        sb.append(response.vendingAmount);
        sb.append(",Enrg Cost:");
        sb.append(response.energyAmount);
        sb.append(",Total Charge:");
        sb.append(response.feeAmount);

        return sb.toString();
    }

    private PrepaidBillToken preparePrepaidBillToken(DescoUnifiedResponse response, String company, int billPayTabelId, String customerNo, String mfsTxnId) throws Exception {
        PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setTokenNo(response.token);
        prepaidBillToken.setSeqNo(response.seq);
        prepaidBillToken.setMeterNo(response.meterNo);
        prepaidBillToken.setCompanyCode(company);
        prepaidBillToken.setVendAmnt(response.vendingAmount);
        prepaidBillToken.setEngAmnt(response.energyAmount);
        prepaidBillToken.setTotalCost(response.feeAmount);
        prepaidBillToken.setBillPayTableId(billPayTabelId);
        Map<String, Object> feesMap = new HashMap<>();
        if (customerNo != null) {
            feesMap.put("customerNo", customerNo);
        }
        feesMap.put("mfsTxnId", mfsTxnId);
        feesMap.put("dues", response.dues);
        feesMap.put("customerName", response.accountName);
        feesMap.put("accountNumber", response.accountNo);
        feesMap.put("tariffCode", response.tariffProgram);
        feesMap.put("feeAMT", response.feeAmount);
        feesMap.put("refCode", response.transId);
        feesMap.put("meter_type", "unified");
        Map<String, Object> chargesMap = new HashMap<>();
        if (response.fee != null && response.fee.items != null) {
            response.fee.items.forEach(item -> {
                if ("VAT".equals(item.name)) {
                    feesMap.put(item.name.toLowerCase(), Double.parseDouble(item.amt));
                } else {
                    chargesMap.put(item.name, Double.parseDouble(item.amt));
                    feesMap.put(item.name, item.amt);
                }
            });
        }
        feesMap.put("charges", chargesMap);
        prepaidBillToken.setFees(Json.toJson(feesMap));
        return prepaidBillToken;
    }

    private DescoUnifiedResponse validateAccount(String accountNo) {
        DescoUnifiedRequest request = prepareRequest(accountNo, null, null);
        return executeDescoAPI(request, accountValidationUrl);
    }

    private String validateAmount(String accountNo, String txnId, String amount, String custMsisdn) {
        DescoUnifiedRequest request = prepareRequest(accountNo, txnId, amount);
        request.phone = custMsisdn;
        try {
            DescoUnifiedResponse verifyAccountResponse = executeDescoAPI(request, amountValidationUrl);
            if (verifyAccountResponse != null && verifyAccountResponse.state == 200) {
                return null;
            }
            if (Arrays.stream(descoResponseCodeList).anyMatch(i -> i == verifyAccountResponse.state)) {
                return verifyAccountResponse.message;
            } else {
                return "Invalid amount";
            }
        } catch (Throwable e) {
            logger.error("Validation Amount Failed:: ", e);
            return "Unable to validate amount";
        }
    }

    private DescoUnifiedResponse payToDesco(String accountNo, String txnId, String amount, String custMsisdn) {
        DescoUnifiedRequest request = prepareRequest(accountNo, txnId, amount);
        request.calcMode = "";
        request.mobile = custMsisdn;
        return executeDescoAPI(request, paymentUrl);
    }

    private DescoUnifiedResponse collectAcknowledgement(String accountNo, String txnId, String amount) {
        DescoUnifiedRequest request = prepareRequest(accountNo, txnId, amount);
        request.refCode = txnId;
        request.vendingMode = "";
        return executeDescoAPI(request, ackUrl);
    }

    private DescoUnifiedResponse executeDescoAPI(DescoUnifiedRequest request, String url) {
        HttpClient httpClient = new HttpClient();
        httpClient.setTimeout(timeout);
        if (useProxy) {
            httpClient.setDefaultProxy();
        }
        try {
            httpClient.setPayloadLoggerInterceptor((HttpClient.HttpRequestSnapshot r) -> {
                try {
                    return "reqXml being sent as -> " + URLDecoder.decode(r.body.substring(7), "UTF-8").replaceAll("(userPass|apiKey)=\"([^\"]+)\"", "$1=\"************\"");
                } catch (UnsupportedEncodingException e) {
                    return r.body;
                }
            });
            return httpClient.postForEntity(url, HttpClient.serializeMap(new HashMap() {{
                put("reqXml", Xml.toXml(request));
            }}), new HashMap() {{
                put("Content-Type", "application/x-www-form-urlencoded");
            }}, DescoUnifiedResponse.class);
        } catch (Throwable t) {
            logger.error("Couldn't invoke API", t);
            return null;
        }
    }

    private DescoUnifiedRequest prepareRequest(String accountNo, String txnId, String amount) {
        DescoUnifiedRequest request = new DescoUnifiedRequest();
        request.userName = userName;
        request.userPass = password;
        request.apiKey = apiKey;
        request.accountNo = accountNo;
        request.transId = txnId;
        request.amount = amount;
        return request;
    }

    private StringBuilder buildSuccessfulText(DescoUnifiedResponse response, String meterNo, double amount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your DESCO prepaid meter recharge request is accepted for Account No: ");
        sb.append(meterNo);
        sb.append(", Amount : ");
        sb.append(amount);
        if (response.energyAmount != null) {
            sb.append(", Energy cost : ");
            sb.append(response.energyAmount);
        }
        sb.append(";");
        return sb;
    }
    
    private StringBuilder buildSuccessfulBanglaText(DescoUnifiedResponse response, String meterNo, double amount) {
        StringBuilder sb = new StringBuilder();
        sb.append("আপনার ডেসকো প্রিপেইড মিটার রিচার্জের অনুরোধ অ্যাকাউন্ট নম্বর ");
        sb.append(meterNo);
        sb.append("এর জন্য গৃহীত হয়েছে। পরিমাণ: ");
        sb.append(amount);
        if (response.energyAmount != null) {
            sb.append(", এনার্জি খরচ: ");
            sb.append(response.energyAmount);
        }
        sb.append("।");
        return sb;
    }

    private void sendSuccessfulSms(DescoUnifiedResponse response, String msisdn, String custMsisdn, String mfsTxnId) {
        StringBuilder sb = new StringBuilder();
        sb.append("DESCO Prepaid Token(s):").append("\n").append(response.token).append("\n");
        sb.append("Sequence:").append(response.seq).append("\n");
        sb.append("Meter No: ").append(response.meterNo).append("\n");
        sb.append("Account No: ").append(response.accountNo).append("\n");
        sb.append("Account Name: ").append(response.accountName).append("\n");
        if (custMsisdn != null) {
            sb.append("Customer No: ").append(custMsisdn).append("\n");
        }
        sb.append("Vending Amount: ").append(response.vendingAmount).append(" Tk").append("\n");
        sb.append("Energy Amount: ").append(response.energyAmount).append(" Tk").append("\n");
        sb.append("Dues: ").append(response.dues).append(" Tk").append("\n");
        if (response.fee != null) {
            response.fee.items.forEach((item) -> {
                sb.append(item.name).append(": ").append(item.amt).append(" Tk").append('\n');
            });
        }
        sb.append("TrxID: ").append(mfsTxnId);
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        
        StringBuilder bsb = new StringBuilder();
        bsb.append("ডেসকো প্রিপেইড টোকেন: ").append("\n").append(response.token).append("\n");
        bsb.append("সিকুয়েন্স: ").append(response.seq).append("\n");
        bsb.append("মিটার নম্বর: ").append(response.meterNo).append("\n");
        bsb.append("অ্যাকাউন্ট নম্বর: ").append(response.accountNo).append("\n");
        bsb.append("অ্যাকাউন্ট নাম: ").append(response.accountName).append("\n");
        if (custMsisdn != null) {
            bsb.append("গ্রাহকের মোবাইল: ").append(custMsisdn).append("\n");
        }
        bsb.append("ভেন্ডিং পরিমাণ: ").append(response.vendingAmount).append(" Tk").append("\n");
        bsb.append("এনার্জি খরচ: ").append(response.energyAmount).append(" Tk").append("\n");
        bsb.append("বকেয়া পরিমাণ: ").append(response.dues).append(" Tk").append("\n");
        if (response.fee != null) {
            response.fee.items.forEach((item) -> {
                bsb.append(item.name).append(": ").append(item.amt).append(" Tk").append('\n');
            });
        }
        bsb.append("TrxID: ").append(mfsTxnId);
        
        logger.debug("SMS body:: " + sb.toString());
        logger.debug("SMS msisdn::" + smsToSend);
        
        SmsUtil.sendSms(smsToSend, sb.toString(),bsb.toString(), true);
    }

    private void sendDisputeSms(DescoUnifiedResponse response, String meterNo, double amount, String msisdn, String custMsisdn) {
        StringBuilder sb = buildSuccessfulText(response, meterNo, amount);
        sb.append(" Meter will be recharged soon. For any further queries please contact DESCO");
        String smsBody = sb.toString();
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        logger.debug("SMS body:: " + smsBody);
        logger.debug("SMS msisdn::" + smsToSend);
        
        StringBuilder bsb = buildSuccessfulBanglaText(response, meterNo, amount);
        bsb.append(" মিটার শীঘ্রই রিচার্জ করা হবে। আরও কোন প্রশ্নের জন্য ডেসকো তে যোগাযোগ করুন");

        SmsUtil.sendSms(smsToSend, smsBody,bsb.toString(), true);
    }

    private void sendFailedSms(String accNo, double amount, String msisdn, String custMsisdn) {
        String smsBody = "Your DESCO prepaid meter recharge request of " + amount + " for account " + accNo + " was unsuccessful.";
        String bangla ="মিটার "+accNo+" এর জন্য আপনার ডেসকো প্রিপেইড টোকেন "+amount+" টাকা প্রদানের অনুরোধ ব্যর্থ হয়েছে।";
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        logger.debug("SMS body:: " + smsBody);
        logger.debug("SMS msisdn::" + smsToSend);

        SmsUtil.sendSms(smsToSend, smsBody,bangla, true);
    }

    public ConsumerValidationResult validateConsumer(ConsumerValidateRequest request) {
        ConsumerValidationResult response = new ConsumerValidationResult();
        response.valid = false;
        try {
            DescoUnifiedResponse descoUnifiedResponse = validateAccount(request.consumerId);
            if (descoUnifiedResponse != null && descoUnifiedResponse.state == 200) {
                response.valid = true;
                Map map = new HashMap();
                map.put("transaction_id", descoUnifiedResponse.transId);
                response.data = map;
            }
        } catch (Throwable h) {
            logger.error("Unable to validate account number. ", h);
        }
        return response;
    }

    public AmountValidationResult validateAmount(AmountValidationRequest request) {
        AmountValidationResult result = new AmountValidationResult();
        try {
            String validateAmountMessage = validateAmount(request.consumerId, (String) request.params.get("transaction_id"), String.valueOf(request.amount), request.params.containsKey("customer_msisdn") ? (String) request.params.get("customer_msisdn") : null);
            if (validateAmountMessage != null) {
                result.valid = false;
                result.message = validateAmountMessage;
            } else {
                result.valid = true;
                result.service_charge = mfsService.getServiceCharge(request.msisdn, "DSCOP", (int) request.amount, WalletType.valueOf(request.wallet_type), Channel.valueOf(request.channel));
            }
        } catch (Throwable h) {
            logger.error("Unable to validate amount ", h);
            result.valid = false;
            result.message = "Unable to validate amount.";
        }
        return result;
    }

    public void resolveDispute(DisputeTransaction disputeTransaction) {
        BillPayServiceStatus billPayServiceStatus = disputeTransaction.getBillPayServiceStatus();
        if (!"U".equals(billPayServiceStatus.getAttr1())) {
            return;
        }
        DescoUnifiedResponse response = collectTxnStatus(billPayServiceStatus.getAccountNo(), billPayServiceStatus.getThirdPartyTxnid(), String.valueOf(billPayServiceStatus.getPaidAmount()));
        if (response == null) {
            return;
        }
        if (response.state == 200) {
            DescoUnifiedResponse ackResponse = null;
            try {
                ackResponse = collectAcknowledgement(billPayServiceStatus.getAccountNo(), billPayServiceStatus.getThirdPartyTxnid(), String.valueOf(billPayServiceStatus.getPaidAmount()));
            } catch (Throwable h) {
                logger.error("Error occurred while calling acknowledge API.", h);
            }
            if (ackResponse != null && ackResponse.state == 200) {
                billPayServiceStatus.setThirdPartyTxnStatus(String.valueOf(response.state));
                billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
                billPayServiceStatusRepository.save(billPayServiceStatus);
                try {
                    PrepaidBillToken prepaidBillToken = prepareBillTokenForAckAPI(response, billPayServiceStatus);
                    prepaidBillTokenRepository.save(prepaidBillToken);
                } catch (Throwable h) {
                    logger.error("Could not insert token in db:: ", h);
                }
                sendSuccessfulSmsAfterResolvedDispute(response, billPayServiceStatus);
                return;
            }
            return;
        }
        sendFailedSms(billPayServiceStatus.getAccountNo(), billPayServiceStatus.getPaidAmount(), billPayServiceStatus.getMsisdn(), billPayServiceStatus.getCustomerMsisdn());
        try {
            mfsService.rollbackTransaction(billPayServiceStatus.getMfsTxnid(), "DSCOPBILLPAY", billPayServiceStatus.getCustomerMsisdn() == null ? false : true, billPayServiceStatus);
            billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK);
        } catch (Throwable h) {
            logger.error("Unable to reverse the transaction", h);
            billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
        }
        billPayServiceStatusRepository.save(billPayServiceStatus);
    }

    private PrepaidBillToken prepareBillTokenForAckAPI(DescoUnifiedResponse response, BillPayServiceStatus billPayServiceStatus) throws Exception {
        PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setTokenNo(response.tokens);
        prepaidBillToken.setSeqNo(response.seq);
        prepaidBillToken.setMeterNo(response.meter);
        prepaidBillToken.setCompanyCode("DSCOP");
        prepaidBillToken.setVendAmnt(response.amount);
        prepaidBillToken.setBillPayTableId(billPayServiceStatus.getId());
        return prepaidBillToken;
    }

    private DescoUnifiedResponse collectTxnStatus(String accountNo, String txnId, String amount) {
        DescoUnifiedRequest request = prepareRequest(accountNo, txnId, amount);
        return executeDescoAPI(request, txnStatusUrl);
    }

    private void sendSuccessfulSmsAfterResolvedDispute(DescoUnifiedResponse response, BillPayServiceStatus billPayServiceStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("DESCO Prepaid Token(s):").append("\n").append(response.tokens).append("\n");
        sb.append("Sequence:").append(response.seq).append("\n");
        sb.append("Account No: ").append(response.accountNo).append("\n");
        sb.append("Meter No: ").append(response.meter).append("\n");
        sb.append("Vending Amount: ").append(response.amount);

        String smsToSend = billPayServiceStatus.getMsisdn();
        if (billPayServiceStatus.getCustomerMsisdn() != null) {
            smsToSend += "," + billPayServiceStatus.getCustomerMsisdn();
        }
        logger.debug("SMS body:: " + sb.toString());
        logger.debug("SMS msisdn::" + smsToSend);
        
        StringBuilder bsb = new StringBuilder();
        bsb.append("DESCO  প্রিপেইড টোকেন   :").append("\n").append(response.tokens).append("\n");
        bsb.append("সিকুয়েন্স:").append(response.seq).append("\n");
        bsb.append("হিসাব নাম্বার: ").append(response.accountNo).append("\n");
        bsb.append("মিটার নম্বর: ").append(response.meter).append("\n");
        bsb.append("ভেন্ডিং পরিমাণ: ").append(response.amount);

        SmsUtil.sendSms(smsToSend, sb.toString(), bsb.toString(),true);
    }

    public class DescoUnifiedPaymentState extends PaymentState {
        public String transactionId;
    }
}