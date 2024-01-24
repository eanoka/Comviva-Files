package com.grameenphone.wipro.fmfs.mfs_communicator.service.breb;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.reb.NewTokenAPIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.DisputeTransactionStatus;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.ServiceProcessingError;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.Bill;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.DueBills;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.RebAuthenticationApiDetail;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.RebBillDetail;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.reb.RebAuthenticationApiResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.reb.RebCheckTransactionStatusResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.reb.RebDueBillDetail;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.reb.RebGetBillResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.reb.RebSaveBillPaymentResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.DisputeTransactionRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.breb.AuthenticationApiRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.breb.BillDetailRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.DisputeService;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.breb.RebPostpaidService.RebPaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillFetcher;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayDisputeResolver;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.HttpClient.HttpRequestSnapshot;
import com.grameenphone.wipro.utility.marshal.Json;

@Service
public class RebPostpaidService implements BillFetcher<RebGetBillResponse>, BillPayer<RebPaymentState, RebSaveBillPaymentResponse>, BillPayDisputeResolver {
    public final static String COMPANY_CODE = "BREB";
    public final static String API_CALL_LOCK = "BREB_TOKEN_SYNC_LOCK";

    @Value("${reb_postpaid_bill_query_url}")
    String rebBillQueryUrl;
    @Value("${reb_postpaid_save_bill_query_url}")
    String billSaveUrl;
    @Value("${reb_postpaid_new_token_url}")
    String newTokenUrl;
    @Value("${reb_postpaid_ack_bill_payment_url}")
    String ackBillPaymentUrl;
    @Value("${reb_postpaid_check_txn_status_url}")
    String chkTxnStatusUrl;
    @Value("${reb_postpaid_authentication_url}")
    String authenticationUrl;
    @Value("${reb_postpaid_channelid}")
    String channelId;
    @Value("${reb_postpaid_password}")
    String password;
    @Value("${reb_postpaid_use_proxy}")
    boolean useProxy;

    @Autowired
    PrepaidBillTokenRepository prepaidBillTokenRepository;

    @Autowired
    DisputeTransactionRepository disputeTransactionRepository;

    @Autowired
    BillDetailRepository billDetailRepository;

    @Autowired
    DisputeService disputeService;

    @Autowired
    AuthenticationApiRepository authenticationApiRepository;

    @Bean({"BREB_Bill_Fetcher", "BREB_Bill_Payer", "BREB_BillPayDisputeResolverService"})
    public RebPostpaidService alias() {
        return this;
    }

    @Override
    public RebPaymentState getState() {
        return new RebPaymentState();
    }

    @Override
    public String getCategory() {
        return "ELEC POST";
    }

    @Override
    public DueBills fetchDueBills(String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map map) throws ValidationException {
        synchronized (API_CALL_LOCK) {
            DueBills bills = new DueBills();
            bills.company = COMPANY_CODE;
            bills.consumerId = consumerId;

            RebAuthenticationApiDetail authApiDetail = authenticationApiRepository.findAll().get(0); //There must be one row in database even with garbage row
            return fetchDueBillsInternal(bills, consumerId, msisdn, wallet_type, channel, authApiDetail.access_token, authApiDetail.refresh_token, false);
        }
    }

    @Override
    public RebSaveBillPaymentResponse pay(RebPaymentState paymentState, PaymentRequest request, String mfsChannel) throws ValidationException {
        synchronized (API_CALL_LOCK) {
            paymentState.billMonth = (String) request.params.get("BILL_MONTH");
            paymentState.billYear = (String) request.params.get("BILL_YEAR");

            RebAuthenticationApiDetail autApiDetail = authenticationApiRepository.findAll().get(0);
            paymentState.accessToken = autApiDetail.access_token;
            paymentState.refreshToken = autApiDetail.refresh_token;

            return payInternal(request, paymentState, false);
        }
    }

    @Override
    public PaymentResult convertToGeneric(RebPaymentState state, RebSaveBillPaymentResponse rebResponse, PaymentRequest request) {
        PaymentResult result = new PaymentResult();
        String thirdPartyTxStatus = null;
        try {
            if (state.markAsDispute) {
                result.txnId = state.mfsPaymentResponse.txnid;
                result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
                result.message = "Your payment has been received. Please contact BREB for further confirmation.";
                disputeService.insertDisputeRecord(state.billPayServiceStatus, request.msisdn, request.customer);
                return result;
            } else if (rebResponse == null) {
                mfsService.rollbackTransaction(state.mfsPaymentResponse.txnid, COMPANY_CODE, request.wallet_type.equals(WalletType.RET) ? true : false, state.billPayServiceStatus);
                result.status = state.billPayServiceStatus.getStatus();
                result.txnId = state.mfsPaymentResponse.txnid;
                result.message = "Bill Payment Failed";
                return result;
            } else {
                result.txnId = state.mfsPaymentResponse.txnid;
                result.status = thirdPartyTxStatus = BillPayStatus.SUCCESS;
                result.message = "Your due bill is paid successfully. You will get payment confirmation SMS within short time.";
                return result;
            }
        } finally {
            if (thirdPartyTxStatus != null) {
                state.billPayServiceStatus.setStatus(thirdPartyTxStatus);
            }
            billPayServiceStatusRepository.save(state.billPayServiceStatus);
        }
    }

    @Override
    public void resolveDispute(DisputeTransaction disputeTransaction) {
        synchronized (API_CALL_LOCK) {
            if (disputeTransaction == null) {
                return;
            }
            BillPayServiceStatus billPayServiceStatus = disputeTransaction.getBillPayServiceStatus();
            try {
                RebAuthenticationApiDetail authApiDetails = authenticationApiRepository.findAll().get(0);
                String accessToken = authApiDetails.access_token;
                String refreshToken = authApiDetails.refresh_token;

                WalletType walletType = WalletType.valueOf(billPayServiceStatus.getTransactionType().substring(0, 3));
                RebCheckTransactionStatusResponse checkTransactionStatusResponse = checkTransactionStatus(disputeTransaction, walletType, accessToken, refreshToken, false);
                if (checkTransactionStatusResponse != null) {
                    Boolean rollback = null;
                    switch (checkTransactionStatusResponse.data.TRANSACTION_STATUS_CODE) {
                        case 1266:
                            rollback = true;
                            break;
                        case 1268:
                            rollback = false;
                            break;
                        case 1267:
                            RebPaymentState paymentState = new RebPaymentState();
                            paymentState.accessToken = accessToken;
                            paymentState.refreshToken = refreshToken;
                            RebSaveBillPaymentResponse saveBillPaymentResponse = acknowledgePayment(paymentState, disputeTransaction.getMeterNo(), billPayServiceStatus.getMsisdn(), walletType, billPayServiceStatus.getMfsTxnid(), false);
                            if (saveBillPaymentResponse == null) {
                                if (!paymentState.markAsDispute) {
                                    rollback = true;
                                }
                            } else {
                                rollback = false;
                            }
                            break;
                    }
                    if (rollback) {
                        mfsService.rollbackTransaction(billPayServiceStatus.getMfsTxnid(), COMPANY_CODE, walletType.equals(WalletType.RET), billPayServiceStatus);
                    } else {
                        billPayServiceStatus.setThirdPartyTxnStatus("" + checkTransactionStatusResponse.response.RESPONSE_CODE);
                        billPayServiceStatus.setThirdPartyTxnid("" + checkTransactionStatusResponse.data.TRANSACTION_STATUS_CODE);
                        billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
                        billPayServiceStatusRepository.save(billPayServiceStatus);
                    }
                }
            } catch (Exception e) {
                logger.error("Couldn't parse tokens", e);
                return;
            }
        }
    }

    private RebGetBillResponse fetchDueBillsFromDBCache(String consumerId) throws ValidationException {
        logger.debug("Collecting token from db as token is already collected");
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.add(Calendar.HOUR_OF_DAY, -2);
        RebBillDetail billDetail = billDetailRepository.getCachedBillDetailByAccountNumber(consumerId, nowCalendar.getTime());
        if (billDetail == null) {
            logger.debug("Token was collected from other party");
            throw new ValidationException(RebErrors._1274);
        } else {
            try {
                return Json.fromJson(billDetail.brebResponse, RebGetBillResponse.class);
            } catch (IOException i) {
                logger.error("Couldn't parse stored bill detail", i);
                throw new ServiceProcessingError(RebErrors.BILL_FETCH);
            }
        }
    }

    private RebAuthenticationApiDetail collectNewToken(String refreshToken) {
        NewTokenAPIResponse tokenResponse = invokeNewTokenApi(refreshToken);
        if(tokenResponse == null) {
            return null;
        }
        RebAuthenticationApiDetail authenticationApiDetail = new RebAuthenticationApiDetail();
        switch (tokenResponse.getRESPONSE().getRESPONSE_CODE()) {
            case 1200:
                updateTokenInRebAuthApiCache(authenticationApiDetail.access_token = tokenResponse.getDATA().getTOKEN().getACCESS_TOKEN(), authenticationApiDetail.refresh_token = tokenResponse.getDATA().getTOKEN().getREFRESH_TOKEN());
                return authenticationApiDetail;
            case 1258:
            case 1273:
            case 1271:
                RebAuthenticationApiResponse authApiRes = invokeAuthenticationApi();
                updateTokenInRebAuthApiCache(authenticationApiDetail.access_token = authApiRes.DATA.TOKEN.ACCESS_TOKEN, authenticationApiDetail.refresh_token = authApiRes.DATA.TOKEN.REFRESH_TOKEN);
                return authenticationApiDetail;
            default:
                return null;
        }
    }

    private DueBills fetchDueBillsInternal(DueBills bills, String consumerId, String msisdn, WalletType wallet_type, Channel channel, String accessToken, String refreshToken, boolean secondCall) throws ValidationException {
        RebGetBillResponse rebGetBillResponse = invokeGetBillQueryApi(consumerId, msisdn, wallet_type, accessToken);

        if (rebGetBillResponse == null) {
            throw new ServiceProcessingError(RebErrors.BILL_FETCH);
        }

        switch(rebGetBillResponse.response.response_code) {
            case 1290: // account number not valid
                throw new ValidationException(RebErrors._1290);
            case 1261: // bill payment was interrupted - ack not called
                throw new ValidationException(RebErrors._1261);
            case 1274:
            case 1252: // bill payment already initiated
                rebGetBillResponse = fetchDueBillsFromDBCache(consumerId);
                break;
            case 1278: // no due bill
                return bills;
            case 1200:
                cacheBillDetail(consumerId, rebGetBillResponse);
                break;
            case 1265:
            case 1271:
            case 1275: // Token invalid or expired
                if(secondCall) {
                    throw new ServiceProcessingError(RebErrors.BILL_FETCH);
                }
                RebAuthenticationApiDetail authApiDetail = collectNewToken(refreshToken);
                if(authApiDetail == null) {
                    throw new ServiceProcessingError(RebErrors.BILL_FETCH);
                }
                return fetchDueBillsInternal(bills, consumerId, msisdn, wallet_type, channel, authApiDetail.access_token, authApiDetail.refresh_token, true);
            default:
                throw new ServiceProcessingError(RebErrors.BILL_FETCH);
        }
        populateDueBillResponseModel(bills.bills, rebGetBillResponse, msisdn, wallet_type, channel);
        return bills;
    }

    private void cacheBillDetail(String consumerId, RebGetBillResponse rebGetBillResponse) {
        RebBillDetail billDetail = billDetailRepository.findBySmsAccountNumber(consumerId);
        if (billDetail == null) {
            billDetail = new RebBillDetail();
        }
        try {
            billDetail.brebResponse = Json.toJson(rebGetBillResponse);
            billDetail.smsAccountNumber = consumerId;
            billDetailRepository.save(billDetail);
        } catch (IOException i) {
            logger.error("Couldn't persist bill detail in db", i);
        }
    }

    private void updateTokenInRebAuthApiCache(String accessToken, String refreshToken) {
        RebAuthenticationApiDetail autApiDetail = authenticationApiRepository.findAll().get(0);
        if (null != accessToken) {
            autApiDetail.access_token = accessToken;
        }
        if (null != refreshToken) {
            autApiDetail.refresh_token = refreshToken;
        }
        authenticationApiRepository.save(autApiDetail);
    }

    private void populateDueBillResponseModel(List<Bill> bills, RebGetBillResponse getBillResponse, String msisdn, WalletType wallet_type, Channel channel) {
        RebDueBillDetail detail = new RebDueBillDetail();
        detail.BILL_MONTH = getBillResponse.data.BILL_MONTH + "";
        detail.BILL_YEAR = getBillResponse.data.BILL_YEAR + "";
        detail.DUE_DATE = getBillResponse.data.DUE_DATE;
        detail.ISSUE_DATE = getBillResponse.data.ISSUE_DATE;
        detail.PBS_NAME_E = getBillResponse.data.PBS_NAME_E;
        detail.SMS_AC_NO = getBillResponse.data.SMS_AC_NO;
        detail.BILL_NO = getBillResponse.data.BILL_NO;

        Bill bill = new Bill();
        bill.amount = getBillResponse.data.DUE_AMOUNT;
        bill.billDueDate = getBillResponse.data.DUE_DATE;
        bill.billNo = getBillResponse.data.BILL_NO;
        bill.serviceCharge = mfsService.getServiceCharge(msisdn, COMPANY_CODE, getBillResponse.data.DUE_AMOUNT, wallet_type, channel);
        bill.detail = detail;

        bills.add(bill);
    }

    private RebGetBillResponse invokeGetBillQueryApi(String consumerId, String msisdn, WalletType wallet_type, String access_token) {
        HttpClient httpClient = new HttpClient();
        if (useProxy) {
            httpClient.setDefaultProxy();
        }
        try {
            return httpClient.postForEntity(rebBillQueryUrl, HttpClient.serializeMap(new HashMap() {{
                put("_pPAY_CHANNEL_ID", channelId);
                put("_pACCESS_TOKEN", access_token);
                put("_pPAYER_ID", msisdn);
                put("_pPAYER_TYPE_ID", (wallet_type == WalletType.SUB) ? "3" : "1");
                put("_pSMS_AC_NO", consumerId);
            }}), new HashMap() {{
                put("Content-Type", "application/x-www-form-urlencoded");
            }}, RebGetBillResponse.class);
        } catch (Exception i) {
            logger.debug("Unable to fetch reb detail", i);
            return null;
        }
    }

    private RebSaveBillPaymentResponse invokeSaveBillApi(PaymentRequest request, RebPaymentState paymentState) {
        String mfsTxnId = paymentState.mfsPaymentResponse.txnid;
        String billMonth = paymentState.billMonth;
        if (billMonth.length() < 2) {
            billMonth = "0" + billMonth;
        }
        String finalBillMonth = billMonth;
        String billYear = paymentState.billYear;
        String billAmount = (int) Math.ceil(request.amount) + "";
        String payerTypeId = (request.wallet_type == WalletType.SUB) ? "3" : "1";

        HttpClient httpClient = new HttpClient();
        if (useProxy) {
            httpClient.setDefaultProxy();
        }
        try {
            return httpClient.postForEntity(billSaveUrl, HttpClient.serializeMap(new HashMap() {{
                put("_pPAY_CHANNEL_ID", channelId);
                put("_pACCESS_TOKEN", paymentState.accessToken);
                put("_pPAYER_ID", request.msisdn);
                put("_pPAYER_TYPE_ID", payerTypeId);
                put("_pSMS_AC_NO", request.getConsumerId());
                put("_pBILL_NO", request.bill);
                put("_pPAY_AMOUNT", billAmount);
                put("_pBILL_MONTH", finalBillMonth);
                put("_pBILL_YEAR", billYear);
                put("_pTRANSACTION_ID", mfsTxnId);
            }}), new HashMap() {{
                put("Content-Type", "application/x-www-form-urlencoded");
            }}, RebSaveBillPaymentResponse.class);
        } catch (IOException e) {
            return null;
        }
    }

    private RebSaveBillPaymentResponse invokeAcknowledgeApi(String consumerId, String msisdn, WalletType wallet_type, String access_token, String txnId) {
        HttpClient httpClient = new HttpClient();
        if (useProxy) {
            httpClient.setDefaultProxy();
        }
        try {
            return httpClient.postForEntity(ackBillPaymentUrl, HttpClient.serializeMap(new HashMap() {{
                put("_pPAY_CHANNEL_ID", channelId);
                put("_pPAYER_ID", msisdn);
                put("_pPAYER_TYPE_ID", (wallet_type == WalletType.SUB) ? "3" : "1");
                put("_pACCESS_TOKEN", access_token);
                put("_pSMS_AC_NO", consumerId);
                put("_pTRANSACTION_ID", txnId);
            }}), new HashMap() {{
                put("Content-Type", "application/x-www-form-urlencoded");
            }}, RebSaveBillPaymentResponse.class);
        } catch (IOException i) {
            return null;
        }
    }

    private NewTokenAPIResponse invokeNewTokenApi(String refresh_token) {
        HttpClient httpClient = new HttpClient();
        if (useProxy) {
            httpClient.setDefaultProxy();
        }
        try {
            return httpClient.postForEntity(newTokenUrl, HttpClient.serializeMap(new HashMap() {{
                put("_pPAY_CHANNEL_ID", channelId);
                put("_pREFRESH_TOKEN", refresh_token);
            }}), new HashMap() {{
                put("Content-Type", "application/x-www-form-urlencoded");
            }}, NewTokenAPIResponse.class);
        } catch (IOException i) {
            logger.debug("Could not process new token API", i);
            return null;
        }
    }

    private RebCheckTransactionStatusResponse invokeStatusCheckApi(String consumerId, String msisdn, WalletType wallet_type, String access_token, String txnId) {
        try {
            HttpClient httpClient = new HttpClient();
            if (useProxy) {
                httpClient.setDefaultProxy();
            }
            return httpClient.postForEntity(chkTxnStatusUrl, HttpClient.serializeMap(new HashMap() {{
                put("_pPAY_CHANNEL_ID", channelId);
                put("_pPAYER_ID", msisdn);
                put("_pPAYER_TYPE_ID", (wallet_type == WalletType.SUB) ? "3" : "1");
                put("_pSMS_AC_NO", consumerId);
                put("_pACCESS_TOKEN", access_token);
                put("_pTRANSACTION_ID", txnId);
            }}), new HashMap() {{
                put("Content-Type", "application/x-www-form-urlencoded");
            }}, RebCheckTransactionStatusResponse.class);
        } catch (Exception ex) {
            logger.debug("Failed to invoke status API", ex);
            return null;
        }
    }

    private RebSaveBillPaymentResponse payInternal(PaymentRequest request, RebPaymentState paymentState, boolean repeatCall) throws ValidationException {
        RebSaveBillPaymentResponse response = invokeSaveBillApi(request, paymentState);
        if (response == null) {
            paymentState.markAsDispute = true;
            return null;
        }
        switch (response.getRESPONSE().getRESPONSE_CODE()) {
            case 1200:
                return acknowledgePayment(paymentState, request.getConsumerId(), request.msisdn, request.wallet_type, paymentState.mfsPaymentResponse.txnid, false);
            case 1259:
            case 1260:
                DueBills bills = new DueBills();
                fetchDueBillsInternal(bills, request.getConsumerId(), request.msisdn, request.wallet_type, request.channel, paymentState.accessToken, paymentState.refreshToken, false);
                if(bills.bills.size() == 0) {
                    throw new ValidationException("Bill Already Paid");
                }
                RebAuthenticationApiDetail autApiDetail = authenticationApiRepository.findAll().get(0); //There is possibility to fetch the token again
                paymentState.accessToken = autApiDetail.access_token;
                paymentState.refreshToken = autApiDetail.refresh_token;

                return payInternal(request, paymentState, false);
            case 1275:
            case 1271:
            case 1265:
                if (repeatCall) {
                    paymentState.markAsDispute = false;
                    return null;
                }
                RebAuthenticationApiDetail authenticationApiDetail = collectNewToken(paymentState.refreshToken);
                if(authenticationApiDetail == null) {
                    paymentState.markAsDispute = false;
                    return null;
                }
                paymentState.accessToken = authenticationApiDetail.access_token;
                paymentState.refreshToken = authenticationApiDetail.refresh_token;
                return payInternal(request, paymentState, true);
            default:
                paymentState.markAsDispute = false;
                return null;
        }
    }

    private RebSaveBillPaymentResponse acknowledgePayment(RebPaymentState paymentState, String consumerId, String msisdn, WalletType walletType, String txnId, boolean repeatCall) {
        RebSaveBillPaymentResponse ackAPIResponse = invokeAcknowledgeApi(consumerId, msisdn, walletType, paymentState.accessToken, txnId);
        if (ackAPIResponse == null) {
            paymentState.markAsDispute = true;
            return null;
        }
        switch (ackAPIResponse.getRESPONSE().getRESPONSE_CODE()) {
            case 1200:
                return ackAPIResponse;
            case 1275:
            case 1271:
            case 1265:
                if (repeatCall) {
                    paymentState.markAsDispute = false;
                    return null;
                }
                RebAuthenticationApiDetail authenticationApiDetail = collectNewToken(paymentState.refreshToken);
                if(authenticationApiDetail == null) {
                    paymentState.markAsDispute = true;
                    return null;
                }
                paymentState.accessToken = authenticationApiDetail.access_token;
                paymentState.refreshToken = authenticationApiDetail.refresh_token;
                return acknowledgePayment(paymentState, consumerId, msisdn, walletType, txnId, true);
            default:
                paymentState.markAsDispute = false;
                return null;
        }
    }

    public RebCheckTransactionStatusResponse checkTransactionStatus(DisputeTransaction disputeTransaction, WalletType walletType, String accessToken, String refreshToken, boolean isRepeat) {
        BillPayServiceStatus billPayServiceStatus = disputeTransaction.getBillPayServiceStatus();
        String txnId = billPayServiceStatus.getMfsTxnid();
        String msisdn = billPayServiceStatus.getMsisdn();
        String consumerId = billPayServiceStatus.getAccountNo();
        RebCheckTransactionStatusResponse checkStatusResponse = invokeStatusCheckApi(consumerId, msisdn, walletType, accessToken, txnId);
        if (checkStatusResponse == null) {
            return null;
        } else
            switch (checkStatusResponse.response.RESPONSE_CODE) {
                case 1275:
                case 1271:
                case 1265:
                    if (isRepeat) {
                        return null;
                    }
                    RebAuthenticationApiDetail authenticationApiDetail = collectNewToken(refreshToken);
                    if(authenticationApiDetail == null) {
                        return null;
                    }
                    return checkTransactionStatus(disputeTransaction, walletType, authenticationApiDetail.access_token, authenticationApiDetail.refresh_token, true);
                case 1200:
                    return checkStatusResponse;
                default:
                    return null;
        }
    }

    public class RebPaymentState extends PaymentState {
        public String accessToken;
        public String refreshToken;
        public boolean markAsDispute = false;
        public String billMonth;
        public String billYear;
    }

    private RebAuthenticationApiResponse invokeAuthenticationApi() {
        try {
            HttpClient httpClient = new HttpClient();
            if (useProxy) {
                httpClient.setDefaultProxy();
            }
            httpClient.setPayloadLoggerInterceptor((HttpRequestSnapshot request) -> request.body.replaceAll("&_pPASSWORD=[^&]+", "&_pPASSWORD=************"));
            RebAuthenticationApiResponse rar = httpClient.postForEntity(authenticationUrl, HttpClient.serializeMap(new HashMap() {{
                put("_pPAY_CHANNEL_ID", channelId);
                put("_pPASSWORD", password);
            }}), new HashMap() {{
                put("Content-Type", "application/x-www-form-urlencoded");
            }}, RebAuthenticationApiResponse.class);
            return rar;
        } catch (Exception ex) {
            logger.debug("Unable to invoke Auth API", ex);
            return null;
        }
    }
}