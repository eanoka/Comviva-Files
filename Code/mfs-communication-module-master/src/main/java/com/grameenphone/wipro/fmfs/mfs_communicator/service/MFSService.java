package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.ServiceChargePaidAmount;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.BalanceResponse.Balance;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PinVerificationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PinVerificationResponse.Result;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.AssociationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.MfsResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.MobiquityReversalFirstRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.MobiquityReversalSecondRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.PayToBillerRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.RetailerAssociationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.RetailerPayToBillerRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.SubscriberAssociationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.SubscriberPayToBillerRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.Company;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.mfsreport.MtxWallet;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.single_result.DoubleResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.single_result.IntegerResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.BillPayServiceStatusRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.CompanyRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.mfs.QueryExecutorRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.mfsreport.MtxWalletReportRepository;
import com.grameenphone.wipro.utility.KV;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.HttpClient.HttpRequestSnapshot;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class MFSService {
    @Autowired
    QueryExecutorRepository queryExecutorRepository;

    @Autowired
    com.grameenphone.wipro.fmfs.mfs_communicator.repository.mfsreport.QueryExecutorRepository reportQueryExecutorRepository;

    @Autowired
    BillPayServiceStatusRepository billPayServiceStatusRepository;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    MtxWalletReportRepository mtxWalletReportRepository;

    @Value("${mfs.api.path}")
    String apiPath;

    @Value("${mfs.request.timeout}")
    int apiTimeout;

    @Value("${mfs.payment.reversal.user.id}")
    String reversalUserId;
    @Value("${verify.pin.api}")
    String verifyPinAPI;

    @Value("${mfs.datasource.default.schema}")
    private String defaultSchema;

    protected static final Logger logger = LoggerFactory.getLogger(MFSService.class);

    public String getMfsChannel(Channel fmfsChannel) {
        switch (fmfsChannel.name()) {
            case "WEB":
            case "USSD":
                return fmfsChannel.name();
            case "GPAY":
            case "RTR_APP":
            case "RMS":
            case "Cockpit":
                return "J2ME";
            case "POS":
            case "CBP":
                return "USSD";
        }
        return null;
    }

    public String getUserGrade(String msisdn) {
        MtxWallet mtxWallet = mtxWalletReportRepository.findByMsisdnAndStatus(msisdn, "Y");
        if (mtxWallet == null) {
            return null;
        } else {
            return mtxWallet.userGrade;
        }
    }

    /**
     * @param msisdn must be of 11 digit
     * @return
     */
    public double getServiceCharge(String msisdn, String companyCode, double amount, WalletType walletType, Channel channel) {
        String serviceChargeQuery = "select NVL(FIXED_SERVICE_CHARGE/100,Round((PCT_SERVICE_CHARGE/100)*1)/100) value from " + defaultSchema + ".MTX_SERVICE_CHARGE a, " + defaultSchema + ".MTX_SERV_CHRG_RANGE_DETAILS B, " + defaultSchema + ".MTX_SERVICE_CHARGE_RANGE c where service_type=:wallet_type and PAYER_BEARER=:channel and PAYER_GRADE_CODE= ((SELECT USER_GRADE FROM " + defaultSchema + ".MTX_WALLET WHERE MSISDN=:msisdn and STATUS = 'Y')) and Payee_grade_code =(SELECT USER_GRADE FROM " + defaultSchema + ".MTX_WALLET WHERE MSISDN=:company and STATUS = 'Y') and a.SERVICE_CHARGE_ID=b.SERVICE_CHARGE_ID and b.SERVICE_CHARGE_RANGE_ID=c.SERVICE_CHARGE_RANGE_ID and :amount * 100 between start_range and end_range and a.LAST_VERSION = (select max(last_version) from " + defaultSchema + ".MTX_SERVICE_CHARGE a where  service_type=:wallet_type and PAYER_BEARER=:channel and PAYER_GRADE_CODE= ((SELECT USER_GRADE FROM " + defaultSchema + ".MTX_WALLET WHERE MSISDN=:msisdn and STATUS = 'Y')) and Payee_grade_code =(SELECT USER_GRADE FROM " + defaultSchema + ".MTX_WALLET WHERE MSISDN=:company and STATUS = 'Y'))";
        DoubleResult serviceCharge = reportQueryExecutorRepository.getSingleResult(serviceChargeQuery, DoubleResult.class, new KV("wallet_type", walletType == WalletType.RET ? "RETBILLPAY" : "BILLPAY"), new KV("channel", getMfsChannel(channel)), new KV("amount", amount), new KV("msisdn", msisdn), new KV("company", companyCode));
        return serviceCharge == null ? 0 : serviceCharge.value;
    }

    public <T> List<T> executeQuery(String query, Class<T> rowClass, Object... params) {
        return queryExecutorRepository.getResultList(query, rowClass, params);
    }

    public boolean isCustomerAssociated(String msisdn, String account, String company) {
        String associationQuery = "SELECT count(*) as value FROM " + defaultSchema + ".MNY_UTILITY_SUBSCRIBER t WHERE t.STATUS_ID = 'Y' AND t.MSISDN = ? AND t.account_number = ? AND t.company_code = ?";
        IntegerResult result = queryExecutorRepository.getSingleResult(associationQuery, IntegerResult.class, msisdn, account.replaceAll("[^a-zA-Z0-9]",""), company);
        return result.value > 0;
    }

    public void associateRetailer(String sessionId, String rtrMsisdn, String custMsisdn, String account, String company, String category, String channel) throws IOException {
        RetailerAssociationRequest request = new RetailerAssociationRequest();
        request.msisdn = rtrMsisdn;
        request.category = category;
        request.companyCode = company;
        request.consumerId = account.replaceAll("[^a-zA-Z0-9]","");
        request.custMsisdn = custMsisdn;
        request.sessionId = sessionId;

        associate(request, channel);
    }

    public void associateSubscriber(String sessionId, String msisdn, String account, String company, String category, String channel) throws IOException {
        SubscriberAssociationRequest request = new SubscriberAssociationRequest();
        request.msisdn = msisdn;
        request.category = category;
        request.companyCode = company;
        request.consumerId = account.replaceAll("[^a-zA-Z0-9]","");
        request.sessionId = sessionId;

        associate(request, channel);
    }

    private void associate(AssociationRequest associationRequest, String channel) throws IOException {
        HttpClient httpClient = new HttpClient(apiTimeout);
        httpClient.setPayloadLoggerInterceptor((String url) -> url.replaceAll("&PASSWORD=[^&]+", "&PASSWORD=************"));
        MfsResponse response = httpClient.postForEntity(apiPath + "&REQUEST_GATEWAY_TYPE=" + channel, associationRequest, MfsResponse.class);
        if (response.txnstatus.equals("200")) {
            logger.debug("Association success for Company:" + associationRequest.companyCode + " Account:" + associationRequest.consumerId + " MSISDN: " + associationRequest.msisdn);
        } else {
            logger.debug("Association fail for Company:" + associationRequest.companyCode + " Account:" + associationRequest.consumerId + " MSISDN: " + associationRequest.msisdn);
        }
    }

    public MfsResponse deductWallet(String sessionId, String msisdn, String pin, String account, String bill, double amount, Integer surcharge, String companyCode, String mfsCompanyCode, String category, String channel, boolean isRetailer) throws IOException {
        PayToBillerRequest deductionRequest;
        if(isRetailer) {
            deductionRequest = new RetailerPayToBillerRequest();
        } else {
            deductionRequest = new SubscriberPayToBillerRequest();
        }
        deductionRequest.payerMsisdn = msisdn;
        deductionRequest.pin = pin;
        deductionRequest.billNo = bill;
        deductionRequest.account = account.replaceAll("[^a-zA-Z0-9]","");
        deductionRequest.pref2 = bill;
        deductionRequest.companyCode = mfsCompanyCode;
        deductionRequest.category = category;
        deductionRequest.sessionId = sessionId;
        deductionRequest.surcharge = surcharge;

        Company company = companyRepository.findCompanyByCompanyCode(companyCode);
        deductionRequest.amount = company.supportDecimalAmount ? amount : (Number) (int)Math.ceil(amount);
        HttpClient httpClient = new HttpClient(apiTimeout);
        httpClient.setPayloadLoggerInterceptor((HttpRequestSnapshot x) -> x.body.replaceAll("(?i)<(pin)\\s*>[^<]*</pin>|<(password)\\s*>([^<]*)</password>", "<$1$2>****</$1$2>"));
        httpClient.setPayloadLoggerInterceptor((String url) -> url.replaceAll("&PASSWORD=[^&]+", "&PASSWORD=************"));
        MfsResponse response = httpClient.postForEntity(apiPath + "&REQUEST_GATEWAY_TYPE=" + channel, deductionRequest, MfsResponse.class);
        if (response.txnstatus.equals("200")) {
            logger.debug("Payment success for Company:" + companyCode + " Account:" + account + " MSISDN: " + msisdn);
        } else {
            logger.debug("Payment fail for Company:" + companyCode + " Account:" + account + " MSISDN: " + msisdn);
        }
        return response;
    }

    public Boolean rollbackTransaction(String txnId, String remarks, boolean isRetailer, BillPayServiceStatus billPayServiceStatus) {
        logger.debug("Calling Reversal ....");
        try {
        MobiquityReversalFirstRequest reversalRequest = new MobiquityReversalFirstRequest();
        reversalRequest.setUserId(reversalUserId);
        reversalRequest.setTxnId(txnId);
        reversalRequest.setRemarks(remarks);
        reversalRequest.setRetailer(isRetailer);

        HttpClient httpClient = new HttpClient(apiTimeout);
            httpClient.setPayloadLoggerInterceptor((String url) -> url.replaceAll("&PASSWORD=[^&]+", "&PASSWORD=************"));
            MfsResponse response = httpClient.postForEntity(apiPath + "&REQUEST_GATEWAY_TYPE=USSD", reversalRequest, MfsResponse.class);
        if (!response.txnstatus.equals("200")) {
            logger.debug("First Reversal api transaction status " + response.txnstatus + " for " + txnId);
            billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
            billPayServiceStatusRepository.save(billPayServiceStatus);
            return false;
        }

        logger.debug("Reversal First Api transaction id " + response.txnid);
        MobiquityReversalSecondRequest secondRequest = new MobiquityReversalSecondRequest();
        secondRequest.setTxnId(response.txnid);
        secondRequest.setUserId(reversalUserId);
            response = httpClient.postForEntity(apiPath + "&REQUEST_GATEWAY_TYPE=USSD", secondRequest, MfsResponse.class);

        if (!response.txnstatus.equals("200")) {
            logger.debug("Second Reversal api transaction status " + response.txnstatus + " for " + txnId);
            billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
            billPayServiceStatusRepository.save(billPayServiceStatus);
            return false;
        }

        billPayServiceStatus.setAttr4(response.txnid);
        billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK);
        billPayServiceStatusRepository.save(billPayServiceStatus);
        return true;
        } catch(Exception e) {
            logger.debug("Exception occurred in reversal for " + txnId, e);
            billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
            billPayServiceStatusRepository.save(billPayServiceStatus);
            return false;
        }
    }

    /**
     * @param msisdn 10 digit msisdn
     * @return
     */
    public Balance getBalanceInfo(String msisdn) {
        DoubleResult balance = queryExecutorRepository.getSingleResult("select b.balance/100 as value from " + defaultSchema + ".mtx_wallet w inner join " + defaultSchema + ".mtx_wallet_balances b on w.WALLET_Number = b.WALLET_NUMBER where w.MSISDN = ? and w.STATUS = 'Y'", DoubleResult.class, msisdn);
        return queryExecutorRepository.getSingleResult("select " + balance.value + " balance, time, lastCrAmount, msisdn from (SELECT th.transfer_date time, th.requested_value / 100 lastCrAmount, ti.party_access_id msisdn FROM " + defaultSchema + ".mtx_transaction_header th inner join " + defaultSchema + ".mtx_transaction_items ti on th.transfer_id = ti.transfer_id WHERE ti.party_access_id = ? AND th.transfer_status = 'TS' AND ti.transaction_type = 'MR' ORDER BY ti.transfer_on DESC) where rownum < 2", Balance.class, msisdn);
    }

    public ServiceChargePaidAmount getServiceChargeAndPaidAmount(String mfsTxnId) {
        ServiceChargePaidAmount serviceChargePaidAmount = queryExecutorRepository.getSingleResult("SELECT t.transfer_value/100 as paidAmount, (t.transfer_value - t.REQUESTED_VALUE)/100 as serviceCharge FROM " + defaultSchema + ".mtx_transaction_header t WHERE t.transfer_id = ? and t.transfer_status = 'TS'", ServiceChargePaidAmount.class, mfsTxnId);
        if(serviceChargePaidAmount == null) {
            serviceChargePaidAmount = new ServiceChargePaidAmount();
        }
        return serviceChargePaidAmount;
    }

    public Result verifyPin(String msisdn, WalletType walletType, String pin) {
        Result result = new Result();
        String sessionId = RandomStringUtils.randomNumeric(15);
        try {
            HttpClient httpClient = new HttpClient(apiTimeout);
            httpClient.setPayloadLoggerInterceptor((HttpRequestSnapshot x) -> x.body.replaceAll("(?i)<(pin)\\s*>[^<]*</pin>|<(password)\\s*>([^<]*)</password>", "<$1$2>****</$1$2>"));
            httpClient.setPayloadLoggerInterceptor((String url) -> url.replaceAll("&PASSWORD=[^&]+", "&PASSWORD=************"));

            MfsResponse command = httpClient.postForEntity(apiPath, "<?xml version='1.0'?>" + "<COMMAND> <SESSION_ID>" + sessionId + "</SESSION_ID><TYPE>" + (walletType == WalletType.SUB ? "CBEREQ" : "RBEREQ") + "</TYPE> <MSISDN>" + msisdn + "</MSISDN> <PROVIDER>101</PROVIDER> <PAYID>12</PAYID> <PIN>" + pin + "</PIN> <LANGUAGE1>1</LANGUAGE1> </COMMAND>", Map.of("Content-Type", "application/xml"), MfsResponse.class);
            result.valid = StringUtils.isNotBlank(command.balance);
        } catch (Exception ex) {
            logger.debug("Balance couldn't be retrieved");
            result.valid = false;
        }
        return result;
    }

    //new verify pin method
    public Result verifyPin(PinVerificationRequest request) {
        Result result = new Result();
        HashMap<String, String> payload = new HashMap<>();
        try{
            payload.put("workspace", "BUSINESS");
            payload.put("identifierType", "MSISDN");
            payload.put("language", "en");
            payload.put("authenticationValue", request.pin);
            payload.put("identifierValue", request.msisdn);
            payload.put("isTokenRequired", "Y");
            HttpClient httpClient = new HttpClient();
            Object responseObject = httpClient.postForEntity(verifyPinAPI, payload, Object.class);
            HashMap response = (HashMap) responseObject;
            result.valid = String.valueOf(response.get("status")).equals("200");
        }catch (Exception e){
            logger.error("Error while validating pin:" +e);
            result.valid = false;
        }
        return result;
    }
}