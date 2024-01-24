package com.grameenphone.wipro.fmfs.mfs_communicator.service.nescoPrepaid;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationResponse.AmountValidationResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid.NescoWasionPrepaidResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid.NescoWasionRemoteChargeRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid.PreChargeRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.NescoPrepaidPaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.DisputeService;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.AmountValidator;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayDisputeResolver;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.SmsUtil;
import com.grameenphone.wipro.utility.marshal.Json;

@Service("NSCPP_WASION")
public class NescoWasionPrepaidService implements BillPayer<NescoPrepaidPaymentState, PaymentResult>, AmountValidator, BillPayDisputeResolver {

	protected static final Logger logger = LoggerFactory.getLogger(NescoWasionPrepaidService.class);
	private static final String successMessage = "Your due bill is paid successfully. You will get payment confirmation SMS within short time.";
	private static final String disputeMessage = "Your payment has been received. Please contact NESCO COMMERCIAL DIVISION for further confirmation.";
	private static final String failMessage = "Sorry, Your bill payment could not be processed.";

	@Value("${nesco_wasion_prepaid_request_timeout}")
	int timeout;
	@Value("${nesco_wasion_prepaid_pre_charge_url}")
	String preChargeUrl;
	@Value("${nesco_wasion_prepaid_remote_charge_url}")
	String remoteChargeUrl;
	@Value("${nesco_wasion_prepaid_history_url}")
	String historyUrl;
	@Value("${nesco_wasion_prepaid_proxy_required}")
	Boolean isProxyRequired;

	@Autowired
	NescoWasionPrepaidAccesTokenService tokenService;
	@Autowired
	DisputeService disputeService;
	@Autowired
	PrepaidBillTokenRepository prepaidBillTokenRepository;

	@Override
	public NescoPrepaidPaymentState getState() {
		return new NescoPrepaidPaymentState();
	}

	@Override
	public String getCategory() {
		return "ELEC PRE";
	}

	@Override
	public void validateRequest(NescoPrepaidPaymentState nescoPrepaidPaymentState, PaymentRequest request) throws ValidationException {
		if (!request.amount_pre_validated) {
			NescoWasionPrepaidResponse response = invokePreChargeAPI(request.getConsumerId(), (int) request.amount);
			if (response == null) {
				throw new ValidationException("Unable To Check Customer Number");
			} else if (!"0".equals(response.resultcode)) {
				throw new ValidationException(response.resultdesc);
			} else {
				nescoPrepaidPaymentState.transactionId = response.data.transactionId;
			}
		} else {
			nescoPrepaidPaymentState.transactionId = String.valueOf(request.params.get("transactionId"));
		}
	}

	@Override
	public PaymentResult pay(NescoPrepaidPaymentState nescoPrepaidPaymentState, PaymentRequest request, String mfsChannel) throws IOException {
		NescoWasionRemoteChargeRequest remoteRechargerequest = new NescoWasionRemoteChargeRequest();
		remoteRechargerequest.transactionId = nescoPrepaidPaymentState.transactionId;
		NescoWasionPrepaidResponse nescoPrepaidWasionResponse = payToNescoWasion(remoteRechargerequest);
		
		PaymentResult result = new PaymentResult();
		String thirdPartyTxStatus = null;
		try {
			if (nescoPrepaidWasionResponse == null || ("10012".equals(nescoPrepaidWasionResponse.resultcode) || "10115".equals(nescoPrepaidWasionResponse.resultcode) || "10113".equals(nescoPrepaidWasionResponse.resultcode))) {
				nescoPrepaidWasionResponse = new NescoWasionPrepaidResponse();
				result.txnId = nescoPrepaidPaymentState.mfsPaymentResponse.txnid;
				result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
				result.message = disputeMessage;
                disputeService.insertDisputeRecord(nescoPrepaidPaymentState.billPayServiceStatus, request.msisdn, request.customer);
				return result;
			} else if ("0".equals(nescoPrepaidWasionResponse.resultcode)) {
				result.txnId = nescoPrepaidPaymentState.mfsPaymentResponse.txnid;
				result.status = thirdPartyTxStatus = BillPayStatus.SUCCESS;
				result.message = successMessage;
				sendSuccessfulSms(request.customer == null ? request.msisdn : request.customer, nescoPrepaidWasionResponse.data.token);
			} else {
                mfsService.rollbackTransaction(nescoPrepaidPaymentState.mfsPaymentResponse.txnid, request.getCompany() + "BILLPAY", request.wallet_type.equals(WalletType.RET), nescoPrepaidPaymentState.billPayServiceStatus);
				result.status = thirdPartyTxStatus = nescoPrepaidPaymentState.billPayServiceStatus.getStatus();
				result.message = failMessage;
				return result;
			}
		} finally {
			nescoPrepaidPaymentState.billPayServiceStatus.setThirdPartyTxnid(result.vendorTxnId = nescoPrepaidPaymentState.transactionId);
			nescoPrepaidPaymentState.billPayServiceStatus.setThirdPartyTxnStatus(nescoPrepaidWasionResponse.resultcode);
			nescoPrepaidPaymentState.billPayServiceStatus.setStatus(thirdPartyTxStatus);
			nescoPrepaidPaymentState.billPayServiceStatus.setAttr2(nescoPrepaidWasionResponse.data != null ? nescoPrepaidWasionResponse.data.token : null);
			billPayServiceStatusRepository.save(nescoPrepaidPaymentState.billPayServiceStatus);
		}
        storeTokenDetails(nescoPrepaidWasionResponse, (int) nescoPrepaidPaymentState.billPayServiceStatus.getId(), nescoPrepaidPaymentState.billPayServiceStatus.getCustomerMsisdn(), nescoPrepaidPaymentState.billPayServiceStatus.getMfsTxnid());
		return result;

	}

	private NescoWasionPrepaidResponse payToNescoWasion(NescoWasionRemoteChargeRequest request) {
		HttpClient client = new HttpClient(timeout);
		if (isProxyRequired) {
			client.setDefaultProxy();
		}
		NescoWasionPrepaidResponse response = null;
		try {
			response = client.postForEntity(remoteChargeUrl, Json.toJson(request), new HashMap<>() {
				{
					put("access_token", tokenService.getToken());
					put("Content-Type", "application/json");
				}
			}, NescoWasionPrepaidResponse.class);

		} catch (Exception e) {
			logger.error("Error in calling Nesco remote charge URL: ", e);
		}
		return response;
	}

    private void storeTokenDetails(NescoWasionPrepaidResponse NescoPrepaidWasionResponse, int billPayTableId, String customerNo, String mfsTxnId) {
		PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
		prepaidBillToken.setMeterNo(NescoPrepaidWasionResponse.data.meterNo);
		prepaidBillToken.setCompanyCode("NSCPP");
        prepaidBillToken.setTotalCost(String.valueOf(NescoPrepaidWasionResponse.data.fee.containsKey("purchaseAmount") ? NescoPrepaidWasionResponse.data.fee.get("purchaseAmount") : null));
        prepaidBillToken.setEngAmnt(String.valueOf(NescoPrepaidWasionResponse.data.fee.containsKey("energyCost") ? NescoPrepaidWasionResponse.data.fee.get("energyCost") : null));
		prepaidBillToken.setBillPayTableId(billPayTableId);
		prepaidBillToken.setTokenNo(NescoPrepaidWasionResponse.data.token);

		Map<String, Object> feeMap = getGeneralizedFeeMap(NescoPrepaidWasionResponse.data.fee);
		feeMap.put("orderNo", NescoPrepaidWasionResponse.data.orderNo);
		if (customerNo != null) {
			feeMap.put("customerNo", customerNo);
		} 
		feeMap.put("transactionId", NescoPrepaidWasionResponse.data.transactionId);
		feeMap.put("customerName", NescoPrepaidWasionResponse.data.customerName);
		feeMap.put("TrxID", mfsTxnId);
		feeMap.put("meterType", "WASION");
		feeMap.put("meterNumber", NescoPrepaidWasionResponse.data.meterNo);
		try {
			String fees = Json.toJson(feeMap);
			prepaidBillToken.setFees(fees);
		} catch (Exception e) {
			logger.error("Unable to set fees value ", e);
		}
		try {
			prepaidBillTokenRepository.save(prepaidBillToken);
		} catch (Exception e) {
			logger.error("Could not insert token in db ", e);
		}
	}
    
	private Map<String, Object> getGeneralizedFeeMap(Map<String, Object> fees) {
		Map<String, Object> tempFees = new HashMap<>();
		tempFees.put("Energy Cost", fees.get("energyCost"));
		tempFees.put("Demand Charge(30/kW)", fees.get("demandCharge"));
		tempFees.put("Purchase Amount", fees.get("purchaseAmount"));
		tempFees.put("Rebate(1%)", fees.get("rebate"));
		tempFees.put("PFC", fees.get("pfc"));
		tempFees.put("Paid Debt", fees.get("debt"));
		tempFees.put("Meter rent (pp)(40/month)", fees.get("meterRent"));		
		tempFees.put("VAT(5%)", fees.get("vat"));
		tempFees.put("Tax", fees.get("tax"));
		
		fees.clear();
		fees.putAll(tempFees);

		return fees;
	}

	private NescoWasionPrepaidResponse invokePreChargeAPI(String consumerId, int amount) {
		PreChargeRequest request = new PreChargeRequest();
		request.customerNo = consumerId;
		request.payinAmount = amount;

		HttpClient client = new HttpClient(timeout);
		if (isProxyRequired) {
			client.setDefaultProxy();
		}
		NescoWasionPrepaidResponse response = null;
		try {
			String token = tokenService.getToken();
			response = client.postForEntity(preChargeUrl, Json.toJson(request), new HashMap<>() {
				{
					put("access_token", token);
					put("Content-Type", "application/json");
				}
			}, NescoWasionPrepaidResponse.class);

		} catch (Exception e) {
			logger.error("Error in calling Nesco pre charge URL: ", e);
			response = new NescoWasionPrepaidResponse();
			response.resultcode = "";
			response.resultdesc = e.getMessage();
		}
		return response;
	}

	private void sendSuccessfulSms(String custMsisdn, String token) {
		String smsBody = "Your Nesco Prepaid Token is " + token + ". Your Meter Could not be Recharged Automatically. "
				+ "Please contact NESCO COMMERCIAL DIVISION for further confirmation.";
		logger.debug("SMS body:: " + smsBody);
		logger.debug("SMS msisdn::" + custMsisdn);
		String banglaSms="আপনার নেসকো প্রিপেইড টোকেন হল "+ token +" আপনার মিটার স্বয়ংক্রিয়ভাবে রিচার্জ করা যাবে না | আরও নিশ্চিতকরণের জন্য অনুগ্রহ করে নেসকো বাণিজ্যিক বিভাগের সাথে যোগাযোগ করুন।";
				
		SmsUtil.sendSms(custMsisdn, smsBody, banglaSms,true);
	}

	@Override
	public AmountValidationResult validateAmount(AmountValidationRequest request) {
		AmountValidationResult result = new AmountValidationResult();
		result.valid = false;
		NescoWasionPrepaidResponse response = invokePreChargeAPI(request.consumerId, (int) request.amount);
		if (response == null) {
			result.message = "Unable To Check Customer Number";
		} else if ("404".equals(response.resultcode) || "10001".equals(response.resultcode)) {
			result.message = "Invalid Customer Number";
		} else if (!"0".equals(response.resultcode)) {
			result.message = response.resultdesc;
		} else {
			result.valid = true;
			result.message = response.resultdesc;
			Map<String, Object> map = response.data.fee;
			map.put("transactionId", response.data.transactionId);
			map.put("customerNo", response.data.customerNo);
			map.put("customerName", response.data.customerName);
			map.put("meterNo", response.data.meterNo);
			map.put("Purchase Amount", map.get("purchaseAmount"));
			map.remove("purchaseAmount");
			result.data = map;
			try {
				result.service_charge = mfsService.getServiceCharge(request.msisdn, "NSCPP", (int) request.amount, WalletType.valueOf(request.wallet_type), Channel.valueOf(request.channel));
			} catch (Throwable h) {
				logger.error("Unable to collect service charge. ", h);
			}
		}
		return result;
	}

	public void resolveDispute(DisputeTransaction disputeTransaction) {
		BillPayServiceStatus billPayServiceStatus = disputeTransaction.getBillPayServiceStatus();
		NescoWasionRemoteChargeRequest request = new NescoWasionRemoteChargeRequest();
		request.transactionId = billPayServiceStatus.getThirdPartyTxnid();
		
		NescoWasionPrepaidResponse nescoPrepaidWasionResponse = invokeHistoryAPI(request);
		if (nescoPrepaidWasionResponse == null) {
			return;
		} else if ("0".equals(nescoPrepaidWasionResponse.resultcode)) {
			billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
			billPayServiceStatusRepository.save(billPayServiceStatus);
            storeTokenDetails(nescoPrepaidWasionResponse, (int) billPayServiceStatus.getId(), billPayServiceStatus.getCustomerMsisdn(), billPayServiceStatus.getMfsTxnid());
			return;
		} else if ("10001".equals(nescoPrepaidWasionResponse.resultcode) || "10011".equals(nescoPrepaidWasionResponse.resultcode) || "10009".equals(nescoPrepaidWasionResponse.resultcode)) {
			mfsService.rollbackTransaction(billPayServiceStatus.getMfsTxnid(), "NSCPPBILLPAY", billPayServiceStatus.getCustomerMsisdn() == null ? false : true, billPayServiceStatus);
			billPayServiceStatusRepository.save(billPayServiceStatus);
			return;
		} else {
			return;
		}
	}

	private NescoWasionPrepaidResponse invokeHistoryAPI(NescoWasionRemoteChargeRequest request) {
		HttpClient client = new HttpClient(timeout);
		if (isProxyRequired) {
			client.setDefaultProxy();
		}
		NescoWasionPrepaidResponse response = null;
		try {
			String token = tokenService.getToken();
			response = client.postForEntity(historyUrl, Json.toJson(request), new HashMap<>() {
				{
					put("access_token", token);
					put("Content-Type", "application/json");
				}
			}, NescoWasionPrepaidResponse.class);
		} catch (Exception e) {
			logger.error("Error in calling Nesco History URL: ", e);
		}
		return response;
    }
}
