package com.grameenphone.wipro.fmfs.mfs_communicator.service.nescoPrepaid;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationResponse.AmountValidationResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid.NescoPrepaidResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid.NescoWasionPrepaidResponse;
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

@Service
public class NescoPrepaidService implements BillPayer<NescoPrepaidPaymentState, PaymentResult>, AmountValidator, BillPayDisputeResolver {

	protected static final Logger logger = LoggerFactory.getLogger(NescoPrepaidService.class);
	private static final String successMessage = "Your due bill is paid successfully. You will get payment confirmation SMS within short time.";
	private static final String disputeMessage = "Your payment has been received. Please contact NESCO COMMERCIAL DIVISION for further confirmation.";
	private static final String failMessage = "Sorry, Your bill payment could not be processed.";

    @Value("${nesco_prepaid_request_timeout}")
    int timeout;
	@Value("${nesco_prepaid_pre_charge_url}")
	String preChargeUrl;
	@Value("${nesco_prepaid_remote_charge_url}")
	String remoteChargeUrl;
	@Value("${nesco_prepaid_history_url}")
	String historyUrl;
	@Value("${nesco_prepaid_proxy_required}")
	Boolean isProxyRequired;

	@Autowired
	NescoPrepaidAccesTokenService tokenService;
	@Autowired
	DisputeService disputeService;
	@Autowired
	PrepaidBillTokenRepository prepaidBillTokenRepository;
	
    @Autowired
    @Qualifier("NSCPP_WASION")
    NescoWasionPrepaidService nescoWasionPrepaidService;
	
	@Value("${nesco_wasion_prepaid_consumerid_prefix}")
	String wasionPrefixes;

	@Bean({ "NSCPP_Bill_Payer", "NSCPP_Amount_Validator", "NSCPP_BillPayDisputeResolverService" })
	public NescoPrepaidService alias() {
		return this;
	}

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
		
		if (wasionPrefixes.contains(request.getConsumerId().substring(0,2))) {
			nescoWasionPrepaidService.validateRequest(nescoPrepaidPaymentState, request);
			return;
		}
		
		if (!request.amount_pre_validated) {
			NescoPrepaidResponse response = invokePreChargeAPI(request.getConsumerId(), (int) request.amount);
			if (response == null) {
				throw new ValidationException("Unable To Check Customer Number");

			} else if ("404".equals(response.resultcode)) {
				throw new ValidationException("Invalid Customer Number");

			} else if ("10006".equals(response.resultcode) || "10007".equals(response.resultcode)) {
				throw new ValidationException("Invalid Amount");

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
		
		if (wasionPrefixes.contains(request.getConsumerId().substring(0,2))) {
			return nescoWasionPrepaidService.pay(nescoPrepaidPaymentState, request, mfsChannel);
		}
		
		NescoPrepaidResponse nescoPrepaidResponse = payToNesco(nescoPrepaidPaymentState.transactionId);
		
		PaymentResult result = new PaymentResult();
		String thirdPartyTxStatus = null;
		try {
			if (nescoPrepaidResponse == null) {
				nescoPrepaidResponse = new NescoPrepaidResponse();
				result.txnId = nescoPrepaidPaymentState.mfsPaymentResponse.txnid;
				result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
				result.message = disputeMessage;
                disputeService.insertDisputeRecord(nescoPrepaidPaymentState.billPayServiceStatus, request.msisdn, request.customer);
				return result;
			} else if ("0".equals(nescoPrepaidResponse.resultcode) || "10010".equals(nescoPrepaidResponse.resultcode)) {
				result.txnId = nescoPrepaidPaymentState.mfsPaymentResponse.txnid;
				result.status = thirdPartyTxStatus = BillPayStatus.SUCCESS;
				result.message = successMessage;
				if ("10010".equals(nescoPrepaidResponse.resultcode)) {
					sendSuccessfulSms(request.customer == null ? request.msisdn : request.customer, nescoPrepaidResponse.data.token);
				}
			} else {
                    mfsService.rollbackTransaction(nescoPrepaidPaymentState.mfsPaymentResponse.txnid, request.getCompany() + "BILLPAY", request.wallet_type.equals(WalletType.RET), nescoPrepaidPaymentState.billPayServiceStatus);
				result.status = thirdPartyTxStatus = nescoPrepaidPaymentState.billPayServiceStatus.getStatus();
				result.message = failMessage;
				return result;
			}
		} finally {
			nescoPrepaidPaymentState.billPayServiceStatus
					.setThirdPartyTxnid(result.vendorTxnId = nescoPrepaidPaymentState.transactionId);
			nescoPrepaidPaymentState.billPayServiceStatus.setThirdPartyTxnStatus(nescoPrepaidResponse.resultcode);
			nescoPrepaidPaymentState.billPayServiceStatus.setStatus(thirdPartyTxStatus);
			nescoPrepaidPaymentState.billPayServiceStatus
					.setAttr2(nescoPrepaidResponse.data != null ? nescoPrepaidResponse.data.token : null);
			billPayServiceStatusRepository.save(nescoPrepaidPaymentState.billPayServiceStatus);
		}
        storeTokenDetails(nescoPrepaidResponse, (int) nescoPrepaidPaymentState.billPayServiceStatus.getId(), nescoPrepaidPaymentState.billPayServiceStatus.getCustomerMsisdn(), nescoPrepaidPaymentState.billPayServiceStatus.getMfsTxnid());
		return result;
	}

	private NescoPrepaidResponse payToNesco(String transactionId) {
		HttpClient client = new HttpClient(timeout);
		if (isProxyRequired) {
			client.setDefaultProxy();
		}
		NescoPrepaidResponse response = null;
		try {
			response = client.postForEntity(remoteChargeUrl + "?transactionId=" + transactionId, "", new HashMap<>() {
				{
					put("access_token", tokenService.getToken());
					put("Content-Type", "application/json");
				}
			}, NescoPrepaidResponse.class);

		} catch (Exception e) {
			logger.error("Error in calling Nesco remote charge URL: ", e);
		}
		return response;
	}

    private void storeTokenDetails(NescoPrepaidResponse nescoPrepaidResponse, int billPayTableId, String customerNo, String mfsTxnId) {
		PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
		prepaidBillToken.setMeterNo(nescoPrepaidResponse.data.meterNo);
		prepaidBillToken.setCompanyCode("NSCPP");
        prepaidBillToken.setTotalCost(String.valueOf(nescoPrepaidResponse.data.fee.containsKey("Purchase Amount") ? nescoPrepaidResponse.data.fee.get("Purchase Amount") : null));
        prepaidBillToken.setEngAmnt(String.valueOf(nescoPrepaidResponse.data.fee.containsKey("Energy Cost") ? nescoPrepaidResponse.data.fee.get("Energy Cost") : null));
		prepaidBillToken.setBillPayTableId(billPayTableId);
		prepaidBillToken.setTokenNo(nescoPrepaidResponse.data.token);

		Map<String, Object> feeMap = nescoPrepaidResponse.data.fee;
		feeMap.put("orderNo", nescoPrepaidResponse.data.orderNo);
		if (customerNo != null) {
			feeMap.put("customerNo", customerNo);
		}
		feeMap.put("transactionId", nescoPrepaidResponse.data.transactionId);
		feeMap.put("customerName", nescoPrepaidResponse.data.customerName);
		feeMap.put("TrxID", mfsTxnId);
		feeMap.put("meterType", "STAR");
		feeMap.put("meterNumber", nescoPrepaidResponse.data.meterNo);
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

	private NescoPrepaidResponse invokePreChargeAPI(String consumerId, int amount) {
		PreChargeRequest request = new PreChargeRequest();
		request.customerNo = consumerId;
		request.payinAmount = amount;

		HttpClient client = new HttpClient(timeout);
		if (isProxyRequired) {
			client.setDefaultProxy();
		}
		NescoPrepaidResponse response = null;
		try {
			String token = tokenService.getToken();
			response = client.postForEntity(preChargeUrl, Json.toJson(request), new HashMap<>() {
				{
					put("access_token", token);
					put("Content-Type", "application/json");
				}
			}, NescoPrepaidResponse.class);

		} catch (Exception e) {
			logger.error("Error in calling Nesco pre charge URL: ", e);
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
		
		if (wasionPrefixes.contains(request.consumerId.substring(0,2))) {
			return nescoWasionPrepaidService.validateAmount(request);
		}
		
		AmountValidationResult result = new AmountValidationResult();
		result.valid = false;
		NescoPrepaidResponse response = invokePreChargeAPI(request.consumerId, (int) request.amount);
		if (response == null) {
			result.message = "Unable To Check Customer Number";
		} else if ("404".equals(response.resultcode)) {
			result.message = "Invalid Customer Number";
		} else if ("10006".equals(response.resultcode) || "10007".equals(response.resultcode)) {
			result.message = "Invalid Amount";
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
			result.data = map;
			try {
				result.service_charge = mfsService.getServiceCharge(request.msisdn, "NSCPP", (int) request.amount,
						WalletType.valueOf(request.wallet_type), Channel.valueOf(request.channel));
			} catch (Throwable h) {
				logger.error("Unable to collect service charge. ", h);
			}
		}
		return result;
	}

	public void resolveDispute(DisputeTransaction disputeTransaction) {
		
		if (wasionPrefixes.contains(disputeTransaction.getBillPayServiceStatus().getAccountNo().substring(0,2))) {
			nescoWasionPrepaidService.resolveDispute(disputeTransaction);
			return;
		}
		
		BillPayServiceStatus billPayServiceStatus = disputeTransaction.getBillPayServiceStatus();
		NescoPrepaidResponse nescoPrepaidResponse = invokeHistoryAPI(billPayServiceStatus.getThirdPartyTxnid());
		if (nescoPrepaidResponse == null) {
			return;
		} else if ("0".equals(nescoPrepaidResponse.resultcode)) {
			billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
			billPayServiceStatusRepository.save(billPayServiceStatus);
            storeTokenDetails(nescoPrepaidResponse, (int) billPayServiceStatus.getId(), billPayServiceStatus.getCustomerMsisdn(), billPayServiceStatus.getMfsTxnid());
			return;
		}
		nescoPrepaidResponse = payToNesco(billPayServiceStatus.getThirdPartyTxnid());
		if (nescoPrepaidResponse == null) {
			return;
		}
		if ("0".equals(nescoPrepaidResponse.resultcode) || "10010".equals(nescoPrepaidResponse.resultcode)) {
			billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
			billPayServiceStatus.setThirdPartyTxnStatus(nescoPrepaidResponse.resultcode);
			billPayServiceStatus.setAttr2(nescoPrepaidResponse.data != null ? nescoPrepaidResponse.data.token : null);
			billPayServiceStatusRepository.save(billPayServiceStatus);
			if ("10010".equals(nescoPrepaidResponse.resultcode)) {
                sendSuccessfulSms(billPayServiceStatus.getCustomerMsisdn() == null ? billPayServiceStatus.getMsisdn() : billPayServiceStatus.getCustomerMsisdn(), nescoPrepaidResponse.data.token);
			}
            storeTokenDetails(nescoPrepaidResponse, (int) billPayServiceStatus.getId(), billPayServiceStatus.getCustomerMsisdn(), billPayServiceStatus.getMfsTxnid());
			return;
		}
        mfsService.rollbackTransaction(billPayServiceStatus.getMfsTxnid(), "NSCPPBILLPAY", billPayServiceStatus.getCustomerMsisdn() == null ? false : true, billPayServiceStatus);
		billPayServiceStatusRepository.save(billPayServiceStatus);
	}

	private NescoPrepaidResponse invokeHistoryAPI(String transactionId) {
		HttpClient client = new HttpClient(timeout);
		if (isProxyRequired) {
			client.setDefaultProxy();
		}
		NescoPrepaidResponse response = null;
		try {
			String token = tokenService.getToken();
			response = client.postForEntity(historyUrl + "?transactionId=" + transactionId, "", new HashMap<>() {
				{
					put("access_token", token);
					put("Content-Type", "application/json");
				}
			}, NescoPrepaidResponse.class);

		} catch (Exception e) {
			logger.error("Error in calling Nesco History URL: ", e);
		}
		return response;
    }
}