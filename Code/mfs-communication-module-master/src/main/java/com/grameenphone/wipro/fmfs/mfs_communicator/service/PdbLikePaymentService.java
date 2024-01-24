package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.HttpResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.pdb_like_company.PdbAcknowledgementRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.pdb_like_company.PdbPaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.pdb_like_company.PdbPrepaidResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.pdb_like_company.PdbVerifyMeterRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.BillPayServiceStatusRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.NetworkUtil;
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
import org.springframework.web.util.UriComponentsBuilder;

import javax.naming.NamingException;
import javax.xml.bind.UnmarshalException;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdbLikePaymentService implements BillPayer<PaymentState, PdbPrepaidResponse> {
    protected static final Logger logger = LoggerFactory.getLogger(PdbLikePaymentService.class);
    public static final String PDB_SUCCESS_CODE = "0";
    private int[] REVERSAL_CODES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 21, 88};
    private static final String DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm";
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);

    @Bean({"PDBPP_Bill_Payer", "WZPDP_Bill_Payer", "REBP_Bill_Payer"})
    public PdbLikePaymentService alias() {
        return this;
    }

    @Value("${pdb_prepaid_webservice_url_base}")
    String pdbUrlBase;

    @Value("${pdb_prepaid_dns_servers:}")
    String[] pdbDnsServers;

    @Value("${reb_prepaid_webservice_url_base}")
    String rebUrlBase;

    @Value("${wzpdcl_prepaid_webservice_url_base}")
    String wzpdclUrlBase;

    @Value("${pdbpp_like_webservice_username}")
    String userName;

    @Value("${pdbpp_like_webservice_password}")
    String password;

    @Value("${pdbpp_like_webservice_amount_validation}")
    String amountValidationApi;

    @Value("${pdbpp_like_webservice_timeout}")
    Integer timeout;

    @Value("${pdbpp_like_use_proxy}")
    Boolean isProxyRequired;

    @Value("${pdbpp_like_webservice_path_payment}")
    String paymentUrl;

    @Value("${pdbpp_like_token_generation_timeout}")
    Integer tokenGenerationTimeout;

    @Value("${pdbpp_like_webservice_path_ack}")
    String acknowledgementUrl;

    @Autowired
    BillPayServiceStatusRepository billPayServiceStatusRepository;

    @Autowired
    MFSService mfsService;

    @Autowired
    DisputeService disputeService;

    @Autowired
    PrepaidBillTokenRepository prepaidBillTokenRepository;

    @Override
    public String getCategory() {
        return "ELEC PRE";
    }

    @Override
    public void validateRequest(PaymentState state, PaymentRequest request) throws ValidationException {
        if (!verifyAmount(request.getConsumerId(), request.amount, request.getCompany())) {
            throw new ValidationException("Invalid amount");
        }
    }

    @Override
    public PdbPrepaidResponse pay(PaymentState state, PaymentRequest request, String mfsChannel) throws IOException {
        PdbPaymentRequest paymentRequest = new PdbPaymentRequest();
        paymentRequest.userName = userName;
        paymentRequest.userPass = password;
        paymentRequest.amount = ((Double)request.amount).intValue();
        paymentRequest.meterNo = request.getConsumerId();
        paymentRequest.transId = request.bill;
        paymentRequest.calcMode = "M";
        paymentRequest.verifyCode = "";
        paymentRequest.verifyData = "";

        long totalTimeTaken = 0;
        long startTime = System.currentTimeMillis();
        HttpResponse response;
        try {
            response = executePDBWebService(Xml.toXml(paymentRequest), this.paymentUrl, true, tokenGenerationTimeout, isProxyRequired, request.getCompany());
        } catch (Exception e) {
            logger.debug("Failed to pay bill:: " + e.getMessage());
            return null;
        }
        if (response.isError) {
            logger.debug("Failed to validate amount.");
            return null;
        }
        totalTimeTaken = System.currentTimeMillis() - startTime;
        logger.debug("Total time taken in calling pdbp payment-Success : " + totalTimeTaken);

        String stringResponse = response.response;
        stringResponse = stringResponse.replaceAll("&", "");
        return unmarshallToResponse(stringResponse);
    }

    @Override
    public PaymentResult convertToGeneric(PaymentState state, PdbPrepaidResponse paymentResponse, PaymentRequest request) {
        PaymentResult result = new PaymentResult();
        String thirdPartyTxStatus = null;
        try {
            if (paymentResponse == null) {
                paymentResponse = new PdbPrepaidResponse();
                result.txnId = state.mfsPaymentResponse.txnid;
                result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
                disputeService.insertDisputeRecord(state.billPayServiceStatus, request.msisdn, request.customer);
                sendDisputeSMS(request.getConsumerId(), String.valueOf(request.amount), request.msisdn, request.customer, request.getCompany());
                return result;
            } else if (!paymentResponse.state.equals(PDB_SUCCESS_CODE)) {
                if (Arrays.binarySearch(REVERSAL_CODES, Integer.parseInt(paymentResponse.state)) < 0) {
                    result.txnId = state.mfsPaymentResponse.txnid;
                    result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
                    disputeService.insertDisputeRecord(state.billPayServiceStatus, request.msisdn, request.customer);
                    sendDisputeSMS(request.getConsumerId(), String.valueOf(request.amount), request.msisdn, request.customer, request.getCompany());
                    return result;
                } else {
                    sendFailedSms(request.getConsumerId(), String.valueOf(request.amount), request.msisdn, request.customer, request.getCompany());
                    try {
                    mfsService.rollbackTransaction(state.mfsPaymentResponse.txnid, request.getCompany() + "BILLPAY", request.wallet_type.equals(WalletType.RET), state.billPayServiceStatus);
                    } catch (Throwable h) {
                        logger.error("Unable to reverse the transaction", h);
                        state.billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
                    }
                    result.status = thirdPartyTxStatus = state.billPayServiceStatus.getStatus();
                    return result;
                }
            } else {
                result.txnId = state.mfsPaymentResponse.txnid;
                result.status = thirdPartyTxStatus = BillPayStatus.SUCCESS;
                sendSuccessSms(paymentResponse, request.getCompany(), request.msisdn, request.customer, state.mfsPaymentResponse.txnid);
            }
        } finally {
            state.billPayServiceStatus.setThirdPartyTxnid(result.vendorTxnId = paymentResponse.refCode);
            state.billPayServiceStatus.setThirdPartyTxnStatus(paymentResponse.state);
            state.billPayServiceStatus.setStatus(thirdPartyTxStatus);
            state.billPayServiceStatus.setAttr2(paymentResponse.token);
            state.billPayServiceStatus.setAttr3(paymentResponse.seq);
            billPayServiceStatusRepository.save(state.billPayServiceStatus);
        }
        try {
            PrepaidBillToken prepaidBillToken = preparePrepaidBillToken(paymentResponse, request.getCompany(), state.billPayServiceStatus.getId());
            prepaidBillTokenRepository.save(prepaidBillToken);
        } catch (Exception e) {
            logger.error("Could not insert token in db" + e.getMessage());
        }
        logger.debug("Acknowledgement response: " + sendAcknowledgement(paymentResponse, request).response);
        return result;
    }

    private HttpResponse sendAcknowledgement(PdbPrepaidResponse prepaidResponse, PaymentRequest pRequest) {
        PdbAcknowledgementRequest request = new PdbAcknowledgementRequest();
        request.userName = userName;
        request.userPass = password;
        request.amount = ((Double)pRequest.amount).intValue();
        request.meterNo = prepaidResponse.meterNum;
        request.refCode = prepaidResponse.refCode;
        request.transId = prepaidResponse.transID;
        request.vendingMode = "apps";

        HttpResponse response = null;
        try {
            logger.debug("Calling Send Acknowledgement API.");
            response = executePDBWebService(Xml.toXml(request), acknowledgementUrl, false, timeout, isProxyRequired, pRequest.getCompany());
        } catch (Exception e) {
            response.isError = true;
            response.response = "Failed to call Acknowledgement API.";
        }
        if (response == null || response.isError) {
            response.response = "Failed to call Acknowledgement API.";
        }
        return response;
    }

    private boolean verifyAmount(String meterNo, double amount, String company) {
        PdbVerifyMeterRequest request = new PdbVerifyMeterRequest();
        request.userName = userName;
        request.userPass = password;
        request.meterNo = meterNo;
        request.amount = ((Double)amount).intValue();
        request.transId = "";

        HttpResponse response;
        try {
            response = executePDBWebService(Xml.toXml(request), amountValidationApi, false, timeout, isProxyRequired, company);
        } catch (Exception e) {
            logger.debug("Failed to validate amount:: " + e.getMessage());
            return false;
        }
        if (response.isError) {
            logger.debug("Failed to validate amount.");
            return false;
        }
        PdbPrepaidResponse prepaidResponse = unmarshallToResponse(response.response);

        if (prepaidResponse == null) {
            logger.debug("Failed to unmarshal response of amount validation.");
            return false;
        } else if (!prepaidResponse.state.equals(PDB_SUCCESS_CODE)) {
            logger.debug("Invalid response::" + prepaidResponse.message);
            return false;
        } else {
            return true;
        }
    }

    private PdbPrepaidResponse unmarshallToResponse(String xmlBody) {
        PdbPrepaidResponse response = null;
        try {
            response = Xml.fromXml(xmlBody, PdbPrepaidResponse.class);
        } catch (Exception exception) {
            if (exception.getCause() instanceof UnmarshalException) {
                Pattern p = Pattern.compile("\"((?:[^=\"]+\")+[^=\"]+)\"");
                Matcher m = p.matcher(xmlBody);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    m.appendReplacement(sb, "\"" + m.group(1).replace("\"", "&quot;") + "\"");
                }
                m.appendTail(sb);
                try {
                    response = Xml.fromXml(sb.toString(), PdbPrepaidResponse.class);
                } catch (Exception exception2) {
                    logger.debug("UnMarshall Error :: " + exception2.getMessage());
                }
            } else {
                logger.debug("UnMarshall Error :: " + exception.getMessage());
            }
        }
        return response;
    }

    private HttpResponse executePDBWebService(final String xmlBody, String url, boolean throwIfError, int requestTimeout, boolean useProxy, String company) throws Exception {
        try {
            switch(company) {
                case "PDBPP":
                    url = pdbUrlBase + url;
                    if(pdbDnsServers.length > 0) {
                        URL _url = new URL(url);
                        String host = _url.getHost();
                        for(String dnsServer : pdbDnsServers) {
                            try {
                                host = NetworkUtil.resolveIpFromDomainName(host, dnsServer);
                                logger.debug("Found IP: " + host + " from " + dnsServer);
                                break;
                            } catch (NamingException | RuntimeException n) {}
                        }
                        if(_url.getHost().equals(host)) {
                            logger.debug("Unable to find any IP for: " + host);
                            return new HttpResponse(true, "Unable to resolve PDB IP");
                        }
                        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
                        url = builder.host(host).build().toString();
                    }
                    break;
                case "WZPDP":
                    url = wzpdclUrlBase + url;
                    break;
                case "REBP":
                    url = rebUrlBase + url;
                    break;
            }
            HttpClient httpClient = new HttpClient();
            httpClient.setTimeout(requestTimeout);
            if (useProxy) {
                logger.debug("Using default proxy");
                httpClient.setDefaultProxy();
            }
            String response;
            logger.debug("XML Request Body:: " + xmlBody.replaceAll("userPass\\s*=\\s*\"[^\"]*\"", "userPass: \"********\""));
            try {
                response = httpClient.post(url, HttpClient.serializeMap(new HashMap() {{
                    put("reqXml", xmlBody);
                }}), new HashMap() {{
                    put("Content-Type", "application/x-www-form-urlencoded");
                }}, false);
            } catch (Throwable h) {
                logger.error("Error in calling " + url, h);
                return new HttpResponse(true, "Invalid Response");
            } finally {
                logger.debug("XML Response Body:: (" + httpClient.getStatusCode() + ") " + httpClient.getTextResponse());
            }
            return new HttpResponse(response);
        } catch (Exception ex) {
            logger.error("Exception occurred in url calling " + url, ex);
            if (throwIfError) {
                throw ex;
            } else {
                return new HttpResponse(true, "Invalid Response");
            }
        }
    }

    private PrepaidBillToken preparePrepaidBillToken(PdbPrepaidResponse response, String company, long billPayTabelId) throws Exception {
        PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setTokenNo(response.token);
        prepaidBillToken.setSeqNo(response.seq);
        prepaidBillToken.setMeterNo(response.meterNum);
        prepaidBillToken.setCompanyCode(company);
        prepaidBillToken.setVendAmnt(response.vendAMT);
        prepaidBillToken.setEngAmnt(response.engAMT);
        prepaidBillToken.setTotalCost(response.feeAMT);
        prepaidBillToken.setBillPayTableId(billPayTabelId);
        Map<String, Object> feesMap = new HashMap<>();
        feesMap.put("customerName", response.customerName);
        feesMap.put("tariffCode", response.tariffCode);
        feesMap.put("arrearAMT", response.arrearAMT);
        feesMap.put("feeAMT", response.feeAMT);
        feesMap.put("meterType", response.meterType);
        feesMap.put("refCode", response.refCode);
        if (response.fee != null && response.fee.items != null) {
            feesMap.put("fees", response.fee.items);
        }
        prepaidBillToken.setFees(Json.toJson(feesMap));
        return prepaidBillToken;
    }

    private void sendSuccessSms(PdbPrepaidResponse prepaidResponse, String company , String msisdn, String custMsisdn, String mfsTxnId) {
        logger.debug("Sending Detail SMS.");
        StringBuilder sb = new StringBuilder();
        sb.append(company + " Prepaid Token(s):");
        sb.append('\n');
        sb.append(prepaidResponse.token.replaceAll(",", ",\n"));
        sb.append('\n');
        sb.append("Sequence:").append(prepaidResponse.seq);
        sb.append('\n');
        sb.append("Meter No: ").append(prepaidResponse.meterNum);
        sb.append('\n');
        sb.append("Customer Name: ").append(prepaidResponse.customerName);
        sb.append('\n');
        if (custMsisdn != null) {
            sb.append("Customer No: ").append(custMsisdn);
            sb.append('\n');
        }
        sb.append("Vending Amount: ").append(prepaidResponse.vendAMT).append(" Tk");
        sb.append('\n');
        sb.append("Energy Cost: ").append(prepaidResponse.engAMT).append(" Tk");
        sb.append('\n');
        if (prepaidResponse.fee != null) {
            prepaidResponse.fee.items.forEach((item) -> {
                sb.append(item.name).append(": ").append(item.amt).append(" Tk");
                sb.append('\n');
            });
        }
        sb.append("Arrear Amount: ").append(prepaidResponse.arrearAMT).append(" Tk");
        sb.append('\n');
        sb.append("TrxID: ").append(mfsTxnId);

        String smsBody = sb.toString();
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        logger.debug("SMS body:: " + smsBody);
        logger.debug("SMS msisdn::" + smsToSend);
        
        StringBuilder bsb = new StringBuilder();
        bsb.append(company + "  প্রিপেইড টোকেন :");
        bsb.append('\n');
        bsb.append(prepaidResponse.token.replaceAll(",", ",\n"));
        bsb.append('\n');
        bsb.append("সিকুয়েন্স:").append(prepaidResponse.seq);
        bsb.append('\n');
        bsb.append("মিটার নম্বর: ").append(prepaidResponse.meterNum);
        bsb.append('\n');
        bsb.append("গ্রাহকের নাম: ").append(prepaidResponse.customerName);
        bsb.append('\n');
        if (custMsisdn != null) {
            bsb.append("গ্রাহকের মোবাইল: ").append(custMsisdn);
            bsb.append('\n');
        }
        bsb.append("ভেন্ডিং পরিমাণ: ").append(prepaidResponse.vendAMT).append(" Tk");
        bsb.append('\n');
        bsb.append("এনার্জি খরচ: ").append(prepaidResponse.engAMT).append(" Tk");
        bsb.append('\n');
        if (prepaidResponse.fee != null) {
            prepaidResponse.fee.items.forEach((item) -> {
                bsb.append(item.name).append(": ").append(item.amt).append(" Tk");
                bsb.append('\n');
            });
        }
        bsb.append("বকেয়া পরিমাণ: ").append(prepaidResponse.arrearAMT).append(" Tk");
        bsb.append('\n');
        bsb.append("TrxID: ").append(mfsTxnId);

        String banglasmbsbody = bsb.toString();

        SmsUtil.sendSms(smsToSend, smsBody,banglasmbsbody, true);
    }

    private void sendDisputeSMS(String accNo, String amount, String msisdn, String custMsisdn, String company) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your request is being processed for " + company + " prepaid token of Tk ");
        sb.append(amount);
        sb.append(" for meter ");
        sb.append(accNo);
        sb.append(" at ");
        sb.append(simpleDateFormat.format(new Date()));
        sb.append(".Please wait for confirmation.");

        String smsBody = sb.toString();
        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        logger.debug("SMS body:: " + smsBody);
        logger.debug("SMS msisdn::" + smsToSend);
        
        StringBuilder banglasb = new StringBuilder();

		banglasb.append("আপনার মিটার ");
		banglasb.append(accNo);
		banglasb.append(" এর জন্য ");
		banglasb.append(amount);
		banglasb.append(" টাকা ");
		banglasb.append(simpleDateFormat.format(new Date()));
		banglasb.append(" বিপিডিবি  প্রিপেইড টোকেনের  এর অনুরোধটি প্রক্রিয়া করা হচ্ছে। অনুগ্রহ করে নিশ্চিতকরণের জন্য অপেক্ষা করুন।");

        SmsUtil.sendSms(smsToSend, smsBody, banglasb.toString(),true); 
    }

    private void sendFailedSms(String accNo, String amount, String msisdn, String custMsisdn, String company) {
        String smsBody = "Your " + company + " prepaid token request of Tk " + amount
                + " for meter " + accNo + " was unsuccessful.";
        
        String banglaSmsBody = "মিটার "+ accNo + " এর জন্য আপনার "+company +"  প্রিপেইড টোকেন টাকার "+amount+" অনুরোধ ব্যর্থ হয়েছে।";
        

        String smsToSend = msisdn;
        if (custMsisdn != null) {
            smsToSend += "," + custMsisdn;
        }
        logger.debug("SMS body:: " + smsBody);
        logger.debug("SMS msisdn::" + smsToSend);

        SmsUtil.sendSms(smsToSend, smsBody,banglaSmsBody, true);
    }
}