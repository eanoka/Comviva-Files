package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.ServiceChargePaidAmount;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.RequestHistories;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.*;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests.*;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.BillPayServiceStatusRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.breb.RequestHistoriesRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillFetcher;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayDisputeResolver;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.marshal.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("PayBillService")
public class PayBillService implements BillFetcher, BillPayer<PaymentState, PaymentResult>, BillPayDisputeResolver {

    private static final String DATE_TIME_PATTERN = "dd/MM/yyyy HH:mm";

    private static final Logger logger = LoggerFactory.getLogger(PayBillService.class);

    private ObjectMapper jsonMapper;

    @Value("${dpdcpostpaid.use.proxy}")
    private boolean isProxyRequired;

    @Value("${dpdcpostpaid.request.timeout}")
    private int timeout;

    @Value("${due_bill_api}")
    private String DUE_BILL_URL;

    @Value("${pay_bill_api}")
    private String PAY_BILL_URL;

    @Value("${pay_bill_api_timeout}")
    private int apiTimeout;

    @Autowired
    DisputeService disputeService;

    @Autowired
    private PrepaidBillTokenRepository prepaidBillTokenRepository;

    @Autowired
    private BillPayServiceStatusRepository billPayServiceStatusRepo;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
    @Autowired
    private RequestHistoriesRepository requestHistoriesRepository;

    @Override
    public DueBillResponse fetchDueBillList(String company, String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map params) throws ValidationException {
        DueBillResponse postpaidGetBillResponse;
        if (consumerId == null || consumerId.isEmpty())
            throw new ValidationException("Invalid Consumer number in param list");
        GetBillRequest request = new GetBillRequest();
        request.setUtility(company);
        request.setConsumer_id(consumerId);
        request.setThirdParty("aggregator");
        request.setParams(params != null ? params : new HashMap());
        try {
            Object response = executePostWebService(DUE_BILL_URL, "", request, Object.class);
            HashMap mapObject = (HashMap) response;
            if (mapObject.containsKey("code") && Integer.parseInt(String.valueOf(mapObject.get("code"))) == 200) {
                postpaidGetBillResponse = Json.fromJson(jsonMapper, Json.toJson(response), DueBillResponse.class);
                postpaidGetBillResponse.setTimestamp(System.currentTimeMillis());
                for (BillList bill : postpaidGetBillResponse.getBill_list())
                    bill.setService_charge(mfsService.getServiceCharge(msisdn, company, bill.getAmount(), wallet_type, channel));
                if (postpaidGetBillResponse == null) {
                    logger.info("Unable to collect due bill for " + company + " Postpaid.");
                    throw new ValidationException("Unable to collect due bill for " + company + " Postpaid.");
                }
                if (postpaidGetBillResponse.getBill_list() == null || postpaidGetBillResponse.getBill_list().length == 0) {
                    logger.info("No due bills for the consumer number: " + consumerId);
                    throw new ValidationException("No due bills for the consumer number: " + consumerId);
                }
            } else {
                logger.info(mapObject.containsKey("code") ? String.valueOf(mapObject.get("code")) : Integer.parseInt(String.valueOf(mapObject.get("status")))
                        + " : " + mapObject.get("message"));
                throw new ValidationException(mapObject.containsKey("code") ? Integer.parseInt(String.valueOf(mapObject.get("code"))) : Integer.parseInt(String.valueOf(mapObject.get("status"))), String.valueOf(mapObject.get("message")));
            }
        } catch (ValidationException e) {
            logger.error(e.getStatus() + " : ", e.getReason());
            throw new ValidationException(e.getStatus(), e.getReason());
        } catch (Exception e) {
            logger.error("Error while while fetching the due bills.", e);
            throw new ValidationException("Unable to collect due bill for " + company + " Postpaid.");
        }
        return postpaidGetBillResponse;
    }

    private <T> T executePostWebService(String url, String endpoint, Object params, Class<T> clazz) throws IOException {
        HttpClient client = getHttpClient(apiTimeout);

        url = url + "/" + endpoint;
        try {
            return client.postForEntity(url, params, clazz);
        } catch (MismatchedInputException e) {
            logger.error(client.getTextResponse());
        }
        return null;
    }

    private HttpClient getHttpClient(int timeout) {
        HttpClient client = new HttpClient(timeout);
        if (isProxyRequired) {
            client.setDefaultProxy();
        }
        return client;
    }

    @Bean({"Postpaid_Bill_Fetcher", "Postpaid_Bill_Payer", "Postpaid_BillPayDisputeResolverService"})
    public PayBillService alias() {
        return this;
    }

    @Override
    public String getCategory() {
        return "POST_PAID";
    }

    @Override
    public PaymentResult pay(PaymentState state, PaymentRequest request, String mfsChannel) throws IOException {
        PayBillRequest payBillRequest = new PayBillRequest();
        Object responseObject = null;
        HashMap response = null;
        PaymentResult result = new PaymentResult();
        RequestHistories requestHistories = new RequestHistories();
        String revenueStamp = request.params != null && request.params.containsKey("REVENUE_STAMP") ? String.valueOf(request.params.get("REVENUE_STAMP")) : "0";
        payBillRequest.setBearerCode("WEB");
        payBillRequest.setCurrency(101);
        payBillRequest.setDeviceInfo(getDeviceData());
        payBillRequest.setInitiator("sender");
        payBillRequest.setLanguage("en");
        payBillRequest.setPartnerData(getParnerData(request));
        payBillRequest.setReceiver(getReceiverData(request));
        payBillRequest.setRemarks("bulk_bill_payment");
        payBillRequest.setSender(getSenderData(request));
        payBillRequest.setServiceFlowId("BILLPAYOAP");
        try {
            state.billPayServiceStatus = prepareBillPayModel(request.getConsumerId(), request.amount, request.bill, getCategory(), request.getCompany(), null, null, null, null, request.msisdn, request.customer, state.sessionId, request.wallet_type.name(), request.channel.name(), request.initiator, payBillRequest.getPartnerData().getSurcharge() != null ? Double.parseDouble(payBillRequest.getPartnerData().getSurcharge()) : null, payBillRequest.getSender().getPaymentInstruments().get(0).getAmount());
            state.billPayServiceStatus.setStatus("Initiated");
            state.billPayServiceStatus = billPayServiceStatusRepository.save(state.billPayServiceStatus);
            state.serviceCharge = Double.parseDouble(payBillRequest.getPartnerData().getSurcharge() != null ? payBillRequest.getPartnerData().getSurcharge() : "0.0");
            state.paidAmount = payBillRequest.getSender().getPaymentInstruments().get(0).getAmount();
            requestHistories = getRequestHistoryData(requestHistories, request, "INITIATED");
            requestHistoriesRepository.save(requestHistories);
            payBillRequest.setExternalReferenceId(requestHistories.getTxnID());
            responseObject = executePostWebService(PAY_BILL_URL, "", payBillRequest, Object.class);
            response = (HashMap) responseObject;
            System.out.println("PAY BILL MOBIQUITY RESPONSE : " + response);
        } catch (HttpErrorResponseException exception) {
            response = Json.fromJson(exception.getContent(), HashMap.class);
            System.out.println("PAY BILL MOBIQUITY RESPONSE : " + response);
        } catch (Exception exception) {
            logger.error("Error while paying bill: \n URL :" + PAY_BILL_URL + " \n ERROR :" + exception);
            requestHistories = getRequestHistoryData(requestHistories, request, "ERROR01");
            requestHistoriesRepository.save(requestHistories);
            throw new ValidationException("Error while paying bills : " + exception);
        }
        try {
            if (response != null && String.valueOf(response.get("code")).equals("process.fulfilment")) {
                result.txnId = String.valueOf(response.get("orderId"));
                result.status  = BillPayStatus.DISPUTE;
                result.message = "Your order has been submitted successfully";
                PrepaidBillToken prepaidBillToken = null;
                try {
                    state.billPayServiceStatus.setOrderID(String.valueOf(response.get("orderId")));
                    state.billPayServiceStatus.setStatus(BillPayStatus.DISPUTE);
                    state.billPayServiceStatus = billPayServiceStatusRepository.save(state.billPayServiceStatus);
                    requestHistories = getRequestHistoryData(requestHistories, response, "SUCCESS");
                    requestHistoriesRepository.save(requestHistories);
                    prepaidBillToken = preparePrepaidBillToken(request, state.billPayServiceStatus != null ? state.billPayServiceStatus.getId() : null, revenueStamp);
                    prepaidBillTokenRepository.save(prepaidBillToken);
                } catch (Throwable e) {
                    logger.error("could not insert token in db", e);
                }
                return result;
            }
            if (response != null && String.valueOf(response.get("status")).equalsIgnoreCase("FAILED")) {
                ArrayList<HashMap> errors = (ArrayList<HashMap>) response.get("errors");
                result.message = String.valueOf(errors.get(0).get("message"));
                result.status  = BillPayStatus.FAIL;
                result.txnId = String.valueOf(response.get("orderId"));
                result.failCode = String.valueOf(errors.get(0).get("code"));
                state.billPayServiceStatus.setOrderID(String.valueOf(response.get("orderId")));
                state.billPayServiceStatus.setStatus(BillPayStatus.FAIL);
                state.billPayServiceStatus = billPayServiceStatusRepository.save(state.billPayServiceStatus);
                requestHistories = getRequestHistoryData(requestHistories, response, "FAILED");
                requestHistoriesRepository.save(requestHistories);
                return result;
            }
        } catch (Exception e) {
            requestHistories = getRequestHistoryData(requestHistories, request, "ERROR01");
            requestHistoriesRepository.save(requestHistories);
            logger.error("Error occurred while validating paybill response : " + e);
            throw new ValidationException("Error occurred while validating paybill response");
        }
        return result;
    }

    private PaymentRequest fetchDueBills(PaymentRequest request) throws ValidationException {
        DueBillResponse dueBills = fetchDueBillList(request.getCompany(), request.getConsumerId(), request.msisdn, request.wallet_type, request.channel, request.params);
        BillList bill = null;
        if ((dueBills.getBill_list() != null && dueBills.getBill_list().length != 0)) {
            bill = dueBills.getBill_list()[0];
        }

        ObjectMapper oMapper = new ObjectMapper();
        Map<String, Object> map = oMapper.convertValue(bill.getDetail(), Map.class);
        request.params = map;
        request.amount = Double.parseDouble(map.get("TOTAL_BILL_AMOUNT").toString());

        request.amount_pre_validated = true;
        request.consumer_pre_validated = true;

        return request;
    }

    @Override
    public void resolveDispute(DisputeTransaction disputeTransaction) {

    }

    public DeviceInfo getDeviceData() {
        //Default static values are set
        DeviceInfo deviceInfo = new DeviceInfo();
        deviceInfo.setAppVersion(10.2);
        deviceInfo.setDeviceId(990000862471854l);
        deviceInfo.setLattitude(12.971599);
        deviceInfo.setLogitude(77.594566);
        deviceInfo.setMac("00:1B:44:11:3A:B7");
        deviceInfo.setModel("Oneplus10");
        deviceInfo.setNetworkOperator("Orange");
        deviceInfo.setNetworkType("4G");
        deviceInfo.setOs("Android10");
        deviceInfo.setProviderIpAddress("172.56.76.89");
        return deviceInfo;
    }

    public PartnerData getParnerData(PaymentRequest request) {
        PartnerData partnerData = new PartnerData();
        partnerData.setBillAccountNumber(request.getConsumerId());
        partnerData.setConsumer_id(request.getConsumerId());
        partnerData.setBillNumber(request.bill);
        partnerData.setSurcharge(String.valueOf(request.surviceCharge));
        partnerData.setVat(String.valueOf(request.vat));
        partnerData.setOther1(request.params != null ? request.params.toString() : null);
        partnerData.setOther2("");
        partnerData.setBillerName(request.companyName);
        partnerData.setBillerCode(request.getCompany());
        return partnerData;
    }

    public Receiver getReceiverData(PaymentRequest request) {
        Receiver receiver = new Receiver();
        receiver.setIdType("mobileNumber");
        receiver.setIdValue(request.companyMsisdn);
        receiver.setProductId(12);
        return receiver;
    }

    public Sender getSenderData(PaymentRequest request) {
        Sender sender = new Sender();
        sender.setIdType("mobileNumber");
        sender.setIdValue(request.msisdn);
        sender.setMpin(Integer.parseInt(request.pin));
        ArrayList<PaymentInstrument> instruments = new ArrayList<>();
        PaymentInstrument instrument = new PaymentInstrument();
        instrument.setInstrumentType("WALLET");
        instrument.setAmount(1);
        instrument.setProductId(12);
        instruments.add(instrument);
        sender.setPaymentInstruments(instruments);
        sender.setUserRole("Channel");
        return sender;
    }

    private PrepaidBillToken preparePrepaidBillToken(PaymentRequest request, long billPayId, String revenueStamp) throws JsonProcessingException {
        PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setCompanyCode(request.getCompany());
        prepaidBillToken.setBillPayTableId(billPayId);
        String fees = request.params != null ? Json.toJson(request.params) : null;
        prepaidBillToken.setFees(fees);
        return prepaidBillToken;
    }

    private RequestHistories getRequestHistoryData(RequestHistories payload, PaymentRequest request, String tag) {
        if (tag.equalsIgnoreCase("INITIATED")) {
            payload.setTxnID("TXNID" + System.currentTimeMillis());
            payload.setConsumer_id(request.getConsumerId());
            payload.setBillNumber(request.bill);
            payload.setCompany(request.getCompany());
            payload.setRequestType("PAY_BILL");
            payload.setStatus("INITIATED");
            payload.setRequestInitiatedTime(new Timestamp(System.currentTimeMillis()));
        } else if (tag.equalsIgnoreCase("ERROR01")) {
            payload.setStatus("UNEXPECTED_ERROR");
            payload.setMessage("UNEXPECTED_ERROR");
            payload.setRespondedTime(new Timestamp(System.currentTimeMillis()));
        }
        return payload;
    }

    private RequestHistories getRequestHistoryData(RequestHistories payload, HashMap response, String tag) {
        if (tag.equalsIgnoreCase("SUCCESS")) {
            payload.setRespondedTime(new Timestamp(System.currentTimeMillis()));
            payload.setStatusCode("200");
            payload.setStatus("SUCCESS");
            payload.setMessage(String.valueOf(response.get("message")));
            payload.setOrderId(String.valueOf(response.get("orderId")));
            payload.setOrderStatus(String.valueOf(response.get("orderStatus")));
            payload.setServiceRequestId(String.valueOf(response.get("serviceRequestId")));
        } else if (tag.equalsIgnoreCase("FAILED")) {
            ArrayList<HashMap> errors = (ArrayList<HashMap>) response.get("errors");
            payload.setRespondedTime(new Timestamp(System.currentTimeMillis()));
            payload.setStatusCode(String.valueOf(response.get("httpErrorCode")));
            payload.setStatus("FAILED");
            payload.setErrorCode(String.valueOf(errors.get(0).get("code")));
            payload.setMessage(String.valueOf(errors.get(0).get("message")));
            payload.setOrderId(String.valueOf(response.get("orderId")));
            payload.setOrderStatus(String.valueOf(response.get("orderStatus")));
            payload.setServiceRequestId(String.valueOf(response.get("traceId")));
        }
        return payload;
    }

    public void updatePayBillStatus(PayBillCallBackRequest request){
        RequestHistories requestHistories = requestHistoriesRepository.findByOrderId(request.getOrderID());
        BillPayServiceStatus billPayServiceStatus = billPayServiceStatusRepo.findByOrderID(request.getOrderID());

        requestHistories.setMobiquityTxnID(request.getMobiquityTxnID());
        requestHistories.setMobiquityTxnStatus(request.getMobiquityStatus());
        requestHistories.setMobiquityMessage(request.getMobiquityMessage());
        requestHistories.setThirdPartyTxnID(request.getTPTxnID());
        requestHistories.setThirdPartyTxnStatus(request.getTPStatus());
        requestHistories.setThirdPartyMessage(request.getTPMessage());
        requestHistories.setStatus(request.getTPStatus().equalsIgnoreCase("TS") ? "Success" : "Fail");

        billPayServiceStatus.setMfsTxnid(request.getMobiquityTxnID());
        billPayServiceStatus.setMfsTxnStatus(request.getMobiquityStatus());
        billPayServiceStatus.setThirdPartyTxnid(request.getTPTxnID());
        billPayServiceStatus.setThirdPartyTxnStatus(request.getTPStatus());
        billPayServiceStatus.setStatus(request.getTPStatus().equalsIgnoreCase("TS") ? "Success" : "Fail");

        requestHistoriesRepository.save(requestHistories);
        billPayServiceStatusRepo.save(billPayServiceStatus);
    }
}
