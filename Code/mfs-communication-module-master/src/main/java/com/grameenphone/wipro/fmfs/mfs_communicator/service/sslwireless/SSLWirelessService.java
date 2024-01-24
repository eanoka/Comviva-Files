package com.grameenphone.wipro.fmfs.mfs_communicator.service.sslwireless;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.Bill;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.DueBills;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.sslwireless.SSLWirelessApiResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.sslwireless.SSLWirelessBillInfoResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.DisputeTransactionRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.DisputeService;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillFetcher;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayDisputeResolver;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.marshal.Json;

@Service
public class SSLWirelessService implements BillFetcher<SSLWirelessBillInfoResponse>, BillPayer<SSLWirelessService.SSLWirelessPaymentState, SSLWirelessApiResponse>, BillPayDisputeResolver {

	public final static String COMPANY_CODE = "SSLIS";

	@Value("${sslwireless.isp.bill.info.url}")
	String sslwirelessBillInfoUrl;
	@Value("${sslwireless.isp.bill.payment.url}")
	String sslWirelessBillPaymentUrl;
	@Value("${sslwireless.isp.bill.status.url}")
	String sslWirelessBillStatusUrl;

	@Value("${sslwireless.isp.AUTH-KEY}")
	String AUTH_KEY;
	@Value("${sslwireless.isp.STK-CODE}")
	String STK_CODE;

	@Value("${sslwireless.isp.use.proxy}")
	boolean useProxy;

	@Autowired
	PrepaidBillTokenRepository prepaidBillTokenRepository;

	@Autowired
	DisputeTransactionRepository disputeTransactionRepository;

	@Autowired
	DisputeService disputeService;

	@Bean({"SSLIS_Bill_Fetcher", "SSLIS_Bill_Payer", "SSLIS_BillPayDisputeResolverService"})
    public SSLWirelessService alias() {
        return this;
    }

	@Override
	public SSLWirelessPaymentState getState() {
		return new SSLWirelessPaymentState();
	}

	@Override
	public String getCategory() {
		return "INTR";
	}

	@Override
	public String getComvivaBillNumber(PaymentRequest paymentRequest) 
	{
		return paymentRequest.getCompany().substring(paymentRequest.getCompany().indexOf(':')+1, paymentRequest.getCompany().length()) + paymentRequest.bill;
	}
	
	@Override
	public DueBills fetchDueBills(DueBillsRequest request) throws ValidationException {
		String utility = getUtilityPostFix(request.getCompany());
		String bodyParams = HttpClient.serializeMap(new HashMap<String, Object>() {
			{
				put("transaction_id", request.params.get("transaction_id"));
				put("customer_no", request.getConsumerId());
				put("account_no", request.msisdn);
				put("utility_auth_key", utility);
				put("utility_secret_key", Application.environment.getProperty("sslwireless.isp."+ utility +".secret.key"));
			}
		});
		
		SSLWirelessBillInfoResponse getBillResponse = null;
		try {
			getBillResponse = executSSLWebService(sslwirelessBillInfoUrl, bodyParams, SSLWirelessBillInfoResponse.class);
		}catch(Throwable t) {
			logger.error("error", t);
		}
		if (getBillResponse == null) {
            throw new ValidationException("Unable to collect due bill from SSLISP.");
        }
        if (getBillResponse.statusCode != 0) {
            throw new ValidationException(getBillResponse.message);
        }
        DueBills dueBills = new DueBills();
        dueBills.company = request.getCompany();
        dueBills.consumerId = request.getConsumerId();

        Bill bill = new Bill();
        bill.billNo = StringUtil.generateUniqueReference(String.valueOf(new Random(System.currentTimeMillis()).nextInt(100000)));
        try {
            bill.billDueDate = new SimpleDateFormat("dd-MM-yyyy").parse(getBillResponse.data.get("due_date").substring(0,2).trim()+"-"+getBillResponse.data.get("bill_month")+"-"+getBillResponse.data.get("bill_year"));
        } catch (Exception e) {
            logger.error("Unable to parse date.", e);
        }
        bill.amount = (int) Double.parseDouble(getBillResponse.data.get("total_amount"));
        getBillResponse.data.put("transaction_id", getBillResponse.transactionId);
        bill.detail = getBillResponse.data;

        if (bill.amount == 0) {
            bill.serviceCharge = 0.0;
        } else {
            bill.serviceCharge = mfsService.getServiceCharge(request.msisdn, COMPANY_CODE, bill.amount, request.wallet_type, request.channel);
        }
        dueBills.bills.add(bill);

        return dueBills;
	}
	
    private String getUtilityPostFix(String company) {
    	String uitilityPostFix = "";
    	if (company.contains(":")) {
    		uitilityPostFix = company.substring(company.indexOf(':')+1, company.length());
		} else {
			uitilityPostFix = company;
		}
		return uitilityPostFix;
	}

	private <T> T executSSLWebService(String url, String bodyParams, Class<T> clazz) {
        try {
            HttpClient httpClient = new HttpClient();
            if (useProxy) {
                httpClient.setDefaultProxy();
            }
			return httpClient.postForEntity(url + "?" + bodyParams, null, new HashMap<String, String>() {
				{
					put("AUTH-KEY", AUTH_KEY);
					put("STK-CODE", STK_CODE);
				}
			}, clazz);
        } catch (Exception ex) {
            logger.debug("Failed to invoke SSL API", ex);
            return null;
        }
    }
	
	@Override
	public SSLWirelessApiResponse pay(SSLWirelessPaymentState state, PaymentRequest request, String mfsChannel) throws IOException {
		String utility = getUtilityPostFix(request.getCompany());
		String mfsTxnId = state.mfsPaymentResponse.txnid;
		String bodyParams = HttpClient.serializeMap(new HashMap<String, Object>() {
			{
				put("transaction_id", request.params.get("transaction_id"));
				put("core_transaction_id", mfsTxnId);
				put("utility_auth_key", utility);
				put("utility_secret_key", Application.environment.getProperty("sslwireless.isp."+ utility +".secret.key"));
			}
		});
		SSLWirelessApiResponse response = null;
		try {
			logger.debug("Calling SSLISP bill pay API.");
			response = executSSLWebService(sslWirelessBillPaymentUrl, bodyParams, SSLWirelessApiResponse.class);
		} catch (Throwable t) {
            logger.error("Error calling in SSLISP Bill Payment Api", t);
        }
		return response;
	}

	@Override
	public PaymentResult convertToGeneric(SSLWirelessPaymentState sslPaymentState, SSLWirelessApiResponse sslPaymentresponse, PaymentRequest request) {
        PaymentResult result = new PaymentResult();
        String thirdPartyTxStatus = null;
        try {
            if (sslPaymentresponse == null) {
                sslPaymentresponse = new SSLWirelessApiResponse();
                result.txnId = sslPaymentState.mfsPaymentResponse.txnid;
                result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
                result.message = "Your request is being processed. Please wait for confirmation SMS.";
                disputeService.insertDisputeRecord(sslPaymentState.billPayServiceStatus, request.msisdn, request.customer);
                return result;
            } else {
                String responseCode = sslPaymentresponse.statusCode;
                if (responseCode.equals("111")) {
                    result.txnId = sslPaymentState.mfsPaymentResponse.txnid;
                    result.status = thirdPartyTxStatus = BillPayStatus.SUCCESS;
                    result.message = "Your due bill is paid successfully. You will get payment confirmation SMS within short time.";
                } else if (responseCode.equals("777") || responseCode.equals("700")) {
                	sslPaymentresponse = new SSLWirelessApiResponse();
                    result.txnId = sslPaymentState.mfsPaymentResponse.txnid;
                    result.status = thirdPartyTxStatus = BillPayStatus.DISPUTE;
                    result.message = "Your request is being processed. Please wait for confirmation SMS.";
                    disputeService.insertDisputeRecord(sslPaymentState.billPayServiceStatus, request.msisdn, request.customer);
                    return result;
                } else {
                	result.txnId = sslPaymentState.mfsPaymentResponse.txnid;
                    result.status = thirdPartyTxStatus = BillPayStatus.FAIL;
                    result.message = sslPaymentresponse.message;
                    try {
                    	mfsService.rollbackTransaction(sslPaymentState.mfsPaymentResponse.txnid, request.getCompany() + " BILLPAY", request.wallet_type.equals(WalletType.RET), sslPaymentState.billPayServiceStatus);
                        } catch (Throwable h) {
                        logger.error("Unable to reverse the transaction", h);
                        sslPaymentState.billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
                    }
                    return result;
                }
            }
        } finally {
        	sslPaymentState.billPayServiceStatus.setThirdPartyTxnid(result.vendorTxnId = sslPaymentresponse != null && sslPaymentresponse.transactionId != null? sslPaymentresponse.transactionId : String.valueOf(request.params.get("transaction_id")));
			sslPaymentState.billPayServiceStatus.setThirdPartyTxnStatus(sslPaymentresponse != null && sslPaymentresponse.statusCode != null ? "" + sslPaymentresponse.statusCode : "");
            sslPaymentState.billPayServiceStatus.setStatus(thirdPartyTxStatus);
            billPayServiceStatusRepository.save(sslPaymentState.billPayServiceStatus);
        }
        try {
            PrepaidBillToken prepaidBillToken = preparePrepaidBillToken(request, sslPaymentresponse, sslPaymentState.billPayServiceStatus.getId());
            prepaidBillTokenRepository.save(prepaidBillToken);
        } catch (Exception e) {
            logger.error("Could not insert token in db" + e.getMessage());
        }
        return result;
    }

	private PrepaidBillToken preparePrepaidBillToken(PaymentRequest request, SSLWirelessApiResponse sslPaymentresponse, long billpayTableId) {
		PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setMeterNo(request.getConsumerId());
        prepaidBillToken.setCompanyCode(request.getCompany());
        prepaidBillToken.setTotalCost(String.valueOf(request.amount));
        Map<String, Object> feesMap = new HashMap<>();
        feesMap.put("lid", sslPaymentresponse.lid);
        feesMap.put("transaction_id", sslPaymentresponse.transactionId);
        try {
			prepaidBillToken.setFees(Json.toJson(feesMap));
		} catch (JsonProcessingException e) {
			logger.error("Json processing error", e);
		}
        prepaidBillToken.setBillPayTableId(billpayTableId);
		
		return prepaidBillToken;
	}

	public class SSLWirelessPaymentState extends PaymentState {
	}
	
	@Override
	public void resolveDispute(DisputeTransaction disputeTransaction) {
		BillPayServiceStatus billPayServiceStatus = disputeTransaction.getBillPayServiceStatus();
		
		String transaction_id = disputeTransaction.getBillPayServiceStatus().getThirdPartyTxnid();
		String utility = getUtilityPostFix(disputeTransaction.getBillPayServiceStatus().getCompanyCode());

		String bodyParams = HttpClient.serializeMap(new HashMap<String, Object>() {
			{
				put("transaction_id", transaction_id);
				put("core_transaction_id", disputeTransaction.getBillPayServiceStatus().getMfsTxnid());
				put("utility_auth_key", utility);
				put("utility_secret_key", Application.environment.getProperty("sslwireless.isp." + utility + ".secret.key"));
			}
		});
		SSLWirelessApiResponse sslWirelessBillStatusResponse = null;
		try {
			sslWirelessBillStatusResponse = executSSLWebService(sslWirelessBillStatusUrl, bodyParams, SSLWirelessApiResponse.class);
		} catch (Throwable t) {

		}
		if (sslWirelessBillStatusResponse == null) {
			return;
		} else if (sslWirelessBillStatusResponse.statusCode.equals("777") || sslWirelessBillStatusResponse.statusCode.equals("700")) {
			return;
		} else if (sslWirelessBillStatusResponse.statusCode.equals("111")) {
			billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
			billPayServiceStatusRepository.save(billPayServiceStatus);
			PrepaidBillToken prepaidBillToken = prepaidBillTokenRepository.findByBillPayTableId(billPayServiceStatus.getId());
			Map<String, Object> feesMap = new HashMap<>();
		    feesMap.put("lid", sslWirelessBillStatusResponse.lid);
		    feesMap.put("transaction_id", sslWirelessBillStatusResponse.transactionId);
		    try {
		    	prepaidBillToken.setFees(Json.toJson(feesMap));
			} catch (JsonProcessingException e) {
				logger.error("Json processing error", e);
			}
	        prepaidBillTokenRepository.save(prepaidBillToken);
			return;
		} else {
        	logger.error("Rolling back the transaction based on response status code");
            mfsService.rollbackTransaction(billPayServiceStatus.getMfsTxnid(), disputeTransaction.getBillPayServiceStatus().getCompanyCode() + " BILLPAY", (billPayServiceStatus.getCustomerMsisdn() != null ? true : false), billPayServiceStatus);
		}
	}
}
