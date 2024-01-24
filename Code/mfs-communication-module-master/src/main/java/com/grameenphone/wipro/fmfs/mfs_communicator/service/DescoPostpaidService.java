package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.DueBillResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.Bill;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.DueBills;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.GetUnpaidBillDetailResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.GetUnpaidBillDetailResponse.BillDetail;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.BillPayServiceStatusRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.PrepaidBillTokenRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost.DescoPostpaidAccesTokenService;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost.DescoPostpaidBaseResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost.DescoPostpaidBillDetailRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost.DescoPostpaidBillInfoResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost.DescoPostpaidBillListRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost.DescoPostpaidGetBillListDetailResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost.DescoPostpaidPaymentStatusRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost.DescoPostpaidPaymentStatusResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost.DescoPostpaidPaymentStatusResponse.Data;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost.DescoPostpaidSavePaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillFetcher;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayDisputeResolver;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.spring.ContextUtil;

@Service
public class DescoPostpaidService implements BillFetcher, BillPayer<DescoPostpaidService.DescoPaymentState, PaymentResult>, BillPayDisputeResolver {

	BillPayServiceStatusRepository billPayServiceStatusRepository = ContextUtil.getBean(BillPayServiceStatusRepository.class);
	
	private static final String COMPANY_CODE = "DSCO";

	@Value("${desco_postpaid_username}")
	String username;
	@Value("${desco_postpaid_password}")
	String password;
	@Value("${desco_postpaid_request_timeout}")
	int timeout;
	@Value("${desco_postpaid_proxy_required}")
	Boolean isProxyRequired;
	@Value("${desco_postpaid_bankcode}")
	String bankCode;
	@Value("${desco_postpaid_scrollNo}")
	long scrollNo;
	@Value("${desco_postpaid_login_url}")
	String loginUrl;
	@Value("${desco_postpaid_billinfo_url}")
	String billInfoUrl;
	@Value("${desco_postpaid_billsavepayment_url}")
	String savePaymentUrl;
	@Value("${desco_postpaid_billlist_url}")
	String billListUrl;
	@Value("${desco_postpaid_billPaymentStatus_url}")
	String billPaymentStatusUrl;

	String txnDateTime = null;
	
	@Autowired
	DescoPostpaidAccesTokenService tokenService;

	@Autowired
	DisputeService disputeService;

	@Autowired
	PrepaidBillTokenRepository prepaidBillTokenRepository;
	
	@Autowired
    BillPayServiceStatusRepository billPayServiceStatusRepo;

	public static class DescoPaymentState extends PaymentState {
		public Map<String, Object> dueBillDetails;
	}

	@Bean({ "DSCO_Bill_Fetcher", "DSCO_Bill_Payer", "DSCO_BillPayDisputeResolverService"})
	public DescoPostpaidService alias() {
		return this;
	}

	@Override
	public DescoPaymentState getState() {
		return new DescoPaymentState();
	}
	
	@Override
	public String getCategory() {
		return "ELEC POST";
	}

	// Desco Postpaid - Get unpaid bill list
	@Override
	public DueBills fetchDueBills(String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map params) throws ValidationException {
		if (consumerId == null || consumerId.isEmpty()) {
			throw new ValidationException("Invalid Consumer number in param list");
		}
		DescoPostpaidGetBillListDetailResponse billDataListResponse = null;

		DescoPostpaidBillListRequest request = new DescoPostpaidBillListRequest();
		request.accountNo = consumerId;
		request.paymentStatus = "0";
		try {
			billDataListResponse = getBillList(request);
		}catch(Exception e) {
			if(e instanceof HttpErrorResponseException)
			{
				if(((HttpErrorResponseException) e).getStatus() == 401)
				{
					logger.info("Token expired.Login API called");
					tokenService.setTokenExpired(true);
					try {
						billDataListResponse = getBillList(request);
					} catch (Exception ex) {
						logger.info("New token generation exception: ", ex);
					}
				}
			}
			else
			{
				logger.info("Error", e);
			}
		}
		
		if (billDataListResponse == null) {
			logger.info("Unable to collect due bill from Desco Postpaid.");
			throw new ValidationException("Unable to collect due bill from Desco Postpaid.");
		}
		DueBills dueBills = new DueBills();
		dueBills.company = COMPANY_CODE;
		dueBills.consumerId = consumerId;
		
		if (!billDataListResponse.getStatus().equalsIgnoreCase("OK")) {
			logger.info("No due bills for the consumer number: " + consumerId + ". For validation, please recheck the entered customer no.");
			throw new ValidationException("No due bills for the consumer number: " + consumerId + ". For validation, please recheck the entered customer no.");
		} else {
			for (DescoPostpaidGetBillListDetailResponse.Data responseData : billDataListResponse.data) {
				Bill dueBill = new Bill();
				dueBill.billNo = responseData.billNo;
				try {
					dueBill.billDueDate = new SimpleDateFormat("dd-MM-yyyy").parse(responseData.dueDate);
				} catch (Exception ex) {
					logger.error("Unable to parse date: ", ex);
				}
				Date currentDate = null;
				try
				{
					SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy"); 
					Date date = new Date();
					currentDate = formatter.parse(formatter.format(date));
				}
				catch(Exception ex)
				{
					logger.error("Unable to parse date: ", ex);
				}
				
				
				if(dueBill.billDueDate.before(currentDate))
				{
					dueBill.amount = (int) responseData.amount +  (int) responseData.lpc;
				}
				else
				{
					dueBill.amount = (int) responseData.amount;
				}
				dueBill.vat = responseData.vat;
				if (dueBill.amount == 0) {
					dueBill.serviceCharge = 0.0;
				} else {
					dueBill.serviceCharge = mfsService.getServiceCharge(msisdn, COMPANY_CODE, dueBill.amount,
							wallet_type, channel);
				}
				dueBill.detail = responseData;
				dueBills.bills.add(dueBill);
			}
		}
		return dueBills;
	}


	private DescoPostpaidGetBillListDetailResponse getBillList(DescoPostpaidBillListRequest descoPostpaidBillListRequest) throws Exception {
		DueBillResponse response = new DueBillResponse();
		HttpClient client = new HttpClient(timeout);
		if (isProxyRequired) {
			client.setDefaultProxy();
		}
		response = client.postForEntity(billListUrl, Json.toJson(descoPostpaidBillListRequest), new HashMap<>() {
			{
				put("Content-Type", "application/json");
			}
		}, DueBillResponse.class);
		return new DescoPostpaidGetBillListDetailResponse();
	}

	@Override
	public void validateRequest(DescoPaymentState state, PaymentRequest request) throws ValidationException {
		if (!request.amount_pre_validated) {
			BillDetail detail = getBillInfo(request.getConsumerId(), request.msisdn, request.wallet_type, request.channel, request.bill);
			ObjectMapper oMapper = new ObjectMapper();
			Map<String, Object> map = oMapper.convertValue(detail.data.detail, Map.class);
			map.put("billNumber", map.remove("billNo"));
			map.put("cTariff", map.remove("tariff"));
			map.put("paymentVatAmount", map.remove("totalVat"));
			map.put("tranAccount", map.remove("accountNo"));
			map.put("totalPayableAmount", map.remove("totalAmount"));
			map.put("totalPaidAmount", map.remove("totalAmountTobePaid"));

			if((Double)map.get("totalPaidAmount") != request.amount) {
				throw new ValidationException("Bill amount mismatch.");
			}

			state.dueBillDetails = map;
		} else {
			state.dueBillDetails = request.params;
		}
	}
	
	// Desco Postpaid - Save Payment
	@Override
	public PaymentResult pay(DescoPaymentState state, PaymentRequest request, String mfsChannel) throws ValidationException {
		DescoPostpaidSavePaymentRequest descoSavePaymentRequest = new DescoPostpaidSavePaymentRequest();
		descoSavePaymentRequest.billNumber = String.valueOf(state.dueBillDetails.get("billNumber"));
		descoSavePaymentRequest.billToken = String.valueOf(state.dueBillDetails.get("billToken"));
		descoSavePaymentRequest.transactionId = state.mfsPaymentResponse.txnid;
		descoSavePaymentRequest.bankCode = bankCode;
		try {
			long scrollNumber = getLastScrollNumber();
			descoSavePaymentRequest.scrollNo = String.valueOf(scrollNumber);
		} catch (Exception ex) {
			logger.error("Exception in getting Scroll No: ", ex);
		}
		descoSavePaymentRequest.totalPayableAmount = Double.parseDouble(state.dueBillDetails.get("totalPayableAmount").toString());
		descoSavePaymentRequest.totalPaidAmount = Double.parseDouble(state.dueBillDetails.get("totalPaidAmount").toString());
		descoSavePaymentRequest.lpc = Double.parseDouble(state.dueBillDetails.get("lpc").toString());
		descoSavePaymentRequest.paid = 1;
		descoSavePaymentRequest.issueDate = String.valueOf(state.dueBillDetails.get("issueDate"));
		descoSavePaymentRequest.dueDate = String.valueOf(state.dueBillDetails.get("dueDate"));
		descoSavePaymentRequest.cTariff = String.valueOf(state.dueBillDetails.get("cTariff"));
		descoSavePaymentRequest.departmentCode = String.valueOf(state.dueBillDetails.get("departmentCode"));
		descoSavePaymentRequest.paymentAmount = Double.parseDouble(state.dueBillDetails.get("totalPaidAmount").toString());
		descoSavePaymentRequest.paymentVatAmount = Double.parseDouble(state.dueBillDetails.get("paymentVatAmount").toString());
		descoSavePaymentRequest.paymentDate = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
		descoSavePaymentRequest.trackingNo = "";
		descoSavePaymentRequest.stampQty = 0;
		descoSavePaymentRequest.transactionTrackingNo = "";
		descoSavePaymentRequest.tranAccount = String.valueOf(state.dueBillDetails.get("tranAccount"));
		txnDateTime = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
		descoSavePaymentRequest.transactionDateTime = txnDateTime;

		DescoPostpaidBaseResponse savePaymentResponse = null;
		PaymentResult result = new PaymentResult();
		try {
			savePaymentResponse = savePayment(descoSavePaymentRequest);
		} catch (Exception ex) {
			if(ex instanceof HttpErrorResponseException)
			{
				if(((HttpErrorResponseException) ex).getStatus() == 401)
				{
					logger.info("Token expired.Login API called");
					tokenService.setTokenExpired(true);
					try {
						savePaymentResponse = savePayment(descoSavePaymentRequest);
					} catch (Exception e) {
						logger.info("New token generation exception: ", e);
					}
				}
			}
			else
			{
				logger.debug("Exception occured while calling save payment api: ", ex);
			}
		}
		
		String billPayStatus = null;
		try {
			if (savePaymentResponse == null || savePaymentResponse.getStatus() == null || "500".equalsIgnoreCase(savePaymentResponse.getStatusCode())|| "501".equalsIgnoreCase(savePaymentResponse.getStatusCode()) || "503".equalsIgnoreCase(savePaymentResponse.getStatusCode())) {
				result.txnId = state.mfsPaymentResponse.txnid;
				result.status = billPayStatus = BillPayStatus.DISPUTE;
				result.message = "Your request is being processed. Please wait for confirmation SMS.";
				disputeService.insertDisputeRecord(state.billPayServiceStatus, request.msisdn, request.customer);
				return result;
			}
			if (savePaymentResponse.getStatusCode().equals("323")) {
				result.txnId = state.mfsPaymentResponse.txnid;
				result.status = billPayStatus = BillPayStatus.SUCCESS;
				result.message = "Your due bill is paid successfully. You will get payment confirmation SMS within short time.";
				try {
					PrepaidBillToken prepaidBillToken = preparePrepaidBillToken(state);
					prepaidBillTokenRepository.save(prepaidBillToken);
				} catch (Exception ex) {
					logger.error("Could not insert desco postpaid token in db: ", ex);
				}
				return result;
			} else {
				result.txnId = state.mfsPaymentResponse.txnid;
				result.status = billPayStatus = BillPayStatus.FAIL;
				result.message = savePaymentResponse.getMessage();
				try {
					mfsService.rollbackTransaction(state.mfsPaymentResponse.txnid, "Desco Postpaid",
							request.wallet_type.equals(WalletType.RET), state.billPayServiceStatus);
				} catch (Throwable h) {
					logger.error("Unable to reverse the transaction: ", h);
					state.billPayServiceStatus.setStatus(BillPayStatus.ROLLBACK_FAIL);
				}
				return result;
			}
		} finally {
			state.billPayServiceStatus.setStatus(billPayStatus);
			billPayServiceStatusRepository.save(state.billPayServiceStatus);
		}
	}

	private PaymentRequest billDetail(PaymentRequest request) throws ValidationException {
		BillDetail detail = getBillInfo(request.getConsumerId(), request.msisdn, request.wallet_type, request.channel, request.params);
		ObjectMapper oMapper = new ObjectMapper();
		Map<String, Object> map = oMapper.convertValue(detail.data.detail, Map.class);
		map.put("billNumber", map.get("billNo"));
		map.put("cTariff", map.get("tariff"));
		map.put("paymentVatAmount", map.get("totalVat"));
		map.put("tranAccount", map.get("accountNo"));
		map.put("totalPayableAmount", map.get("totalAmount"));
		map.put("totalPaidAmount", map.get("totalAmountTobePaid"));
		request.params = map;

		request.amount_pre_validated = true;
		request.consumer_pre_validated = true;

		return request;
	}

	private DescoPostpaidBaseResponse savePayment(DescoPostpaidSavePaymentRequest request) throws Exception {
		DescoPostpaidBaseResponse response = new DescoPostpaidBaseResponse();
		HttpClient client = new HttpClient(timeout);
		if (isProxyRequired) {
			client.setDefaultProxy();
		}
		response = client.postForEntity(savePaymentUrl, Json.toJson(request), new HashMap<>() {
			{
				put("Content-Type", "application/json");
				put("Authorization", tokenService.getToken());
			}
		}, DescoPostpaidBaseResponse.class);
		return response;
	}

	private PrepaidBillToken preparePrepaidBillToken(DescoPaymentState state) throws JsonProcessingException {
		PrepaidBillToken token = new PrepaidBillToken();
		token.setCompanyCode(COMPANY_CODE);
		token.setBillPayTableId(state.billPayServiceStatus.getId());

		Map<String, Object> feeMap = new HashMap<>();
		feeMap.put("departmentCode", state.dueBillDetails.get("departmentCode"));
		feeMap.put("paymentAmount", state.dueBillDetails.get("totalPaidAmount"));
		feeMap.put("paymentVatAmount", state.dueBillDetails.get("paymentVatAmount"));
		feeMap.put("transactionId", state.mfsPaymentResponse.txnid);
		feeMap.put("billNumber", state.dueBillDetails.get("billNumber"));
		feeMap.put("transactionDateTime", txnDateTime);
		String fees = Json.toJson(feeMap);
		token.setFees(fees);

		return token;
	}

	public long getLastScrollNumber() {
		long currentScrollNumber = 1L;
		String strBuff;
		try {
			String path = System.getProperty("java.io.tmpdir");
			File directoryPath = new File(path);
			String content[] = directoryPath.list();
			Optional<String> findFirst = Arrays.asList(content).stream().filter(file -> file.startsWith("DescoPost_Scroll")).findAny();

			if (findFirst.isPresent()) {
				logger.debug("File: " + path + '/' + findFirst.get());
				File file = new File(path + "/" + findFirst.get());
				if (file.exists()) {
					logger.debug("File exists");
				}
				BufferedReader br = new BufferedReader(new FileReader(file));
				while ((strBuff = br.readLine()) != null) {
					String[] split = strBuff.split(",");
					String fileDate = split[0];
					String fileReadScrollNo = split[1];
					FileWriter fWriter = new FileWriter(file);
					if (fileDate.equals(new SimpleDateFormat("YYYY-MM-dd").format(Calendar.getInstance().getTime()))) {
						currentScrollNumber = Long.parseLong(fileReadScrollNo) + 1;
						fWriter.write(split[0] + "," + String.valueOf(currentScrollNumber));
					} else {
						currentScrollNumber = scrollNo;
						fWriter.write((new SimpleDateFormat("YYYY-MM-dd").format(Calendar.getInstance().getInstance().getTime())) + "," + String.valueOf(currentScrollNumber));
					}
					fWriter.close();
				}
			} else {
				File file = File.createTempFile("DescoPost_Scroll", ".txt");
				logger.debug("File " + file.getName() + " created at path: " + file.getParent() + ": " + file.getParent() + "/" + file.getName());
				FileWriter fWriter = new FileWriter(file.getParent() + "/" + file.getName());
				Calendar currentDateTime = Calendar.getInstance();
				currentScrollNumber = scrollNo;
				fWriter.write((new SimpleDateFormat("YYYY-MM-dd").format(currentDateTime.getInstance().getTime())) + "," + String.valueOf(currentScrollNumber));
				logger.debug("New File " + file.getName() + " created and updated with current date and scroll no set to " + currentScrollNumber);
				fWriter.close();
			}
		} catch (Exception ex) {
			logger.error("Exception in getLatestScrollNo:", ex);
		}
		return currentScrollNumber;
	}

	// Desco Postpaid - Resolve Dispute
	@Override
	public void resolveDispute(DisputeTransaction disputeTransaction) {
		Date txdnate = disputeTransaction.getCreationDate();
        
        BillPayServiceStatus billPayServiceStatus = disputeTransaction.getBillPayServiceStatus();
        
        DescoPostpaidPaymentStatusResponse descoPaymentStatusResponse = null;
        try
        {
        	descoPaymentStatusResponse = paymentStatus(billPayServiceStatus.getBillNo(), billPayServiceStatus.getMfsTxnid());
        }
        catch(Exception ex)
        {
        	if(ex instanceof HttpErrorResponseException)
			{
				if(((HttpErrorResponseException) ex).getStatus() == 401)
				{
					logger.info("Token expired.Login API called");
					tokenService.setTokenExpired(true);
					try {
						descoPaymentStatusResponse = paymentStatus(billPayServiceStatus.getBillNo(), billPayServiceStatus.getMfsTxnid());
					} catch (Exception e) {
						logger.info("New token generation exception: ", e);
					}
				}
			}
			else
			{
				logger.error("Exception in resolveDispute: "+ex);
			}
        }
        
        if (descoPaymentStatusResponse == null || "500".equalsIgnoreCase(descoPaymentStatusResponse.getStatusCode()) || "501".equalsIgnoreCase(descoPaymentStatusResponse.getStatusCode()) || "503".equalsIgnoreCase(descoPaymentStatusResponse.getStatusCode())) {
            logger.error("Unable to get reconciliation response, so leaving the status as is");
            return;
        }
        
        if(descoPaymentStatusResponse.getStatus().equalsIgnoreCase("OK"))
        {
        	try
        	{
        		Data data = descoPaymentStatusResponse.data.get(0);
            	if(data.paymentStatus.equalsIgnoreCase("PAID"))
            	{
            		billPayServiceStatus.setThirdPartyTxnid(data.transactionId);
                    billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
                    billPayServiceStatusRepo.save(billPayServiceStatus);
                    try {
                        PrepaidBillToken prepaidBillToken = preparePrepaidBillToken(data, disputeTransaction.getBillPayServiceStatus().getId());
                        prepaidBillTokenRepository.save(prepaidBillToken);
                    } catch (Throwable e) {
                        logger.error("could not insert desco postpaid token in db", e);
                    }
                    return;
            	}
        	}
        	catch(Exception ex)
        	{
        		logger.error("Exception: " + ex);
        	}
        }
        else {
        	logger.error("Rolling back the transaction based on response status code");
            mfsService.rollbackTransaction(billPayServiceStatus.getMfsTxnid(), COMPANY_CODE + "BILLPAY", (billPayServiceStatus.getCustomerMsisdn() != null ? true : false), billPayServiceStatus);
        }
	}
	
	private PrepaidBillToken preparePrepaidBillToken(Data response, long billPayId) throws JsonProcessingException {

		PrepaidBillToken prepaidBillToken = new PrepaidBillToken();
        prepaidBillToken.setCompanyCode(COMPANY_CODE);
        prepaidBillToken.setBillPayTableId(billPayId);
        
        Map<String, Object> feeMap = new HashMap<>();
        feeMap.put("departmentCode", response.departmentCode);
        feeMap.put("paymentAmount", response.paymentAmount);
        feeMap.put("paymentVatAmount", response.paymentVatAmount);
        feeMap.put("transactionId", response.transactionId);
		feeMap.put("billNumber", response.billNumber);
		feeMap.put("paymentDate", response.paymentDate);
		String fees = Json.toJson(feeMap);
		prepaidBillToken.setFees(fees);

        return prepaidBillToken;
	}

	private DescoPostpaidPaymentStatusResponse paymentStatus(String billNo, String txnId) throws Exception {
		DescoPostpaidPaymentStatusResponse paymentStatusResponse = new DescoPostpaidPaymentStatusResponse();
		HttpClient client = new HttpClient(timeout);
		if (isProxyRequired) {
			client.setDefaultProxy();
		}
		DescoPostpaidPaymentStatusRequest request = new DescoPostpaidPaymentStatusRequest();
		request.billNumber = billNo;
		request.transactionId = txnId;

		paymentStatusResponse = client.postForEntity(billPaymentStatusUrl, Json.toJson(request), new HashMap<>() {
			{
				put("Content-Type", "application/json");
				put("Authorization", tokenService.getToken());
			}
		}, DescoPostpaidPaymentStatusResponse.class);
		return paymentStatusResponse;
	}
	
	@Override
	public BillDetail getBillInfo(String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map map) throws ValidationException {
		return getBillInfo(consumerId, msisdn, wallet_type, channel, (String)map.get("billNumber"));
	}
	// Desco Postpaid - Get Bill Details
	public BillDetail getBillInfo(String consumerId, String msisdn, WalletType wallet_type, Channel channel, String billNumber) throws ValidationException {
		if (consumerId == null || consumerId.isEmpty()) {
			throw new ValidationException("Invalid Consumer number in param list");
		}
		DescoPostpaidBillInfoResponse descoBillDetailResponse = null;
		BillDetail billGetDetail = new BillDetail();
		DescoPostpaidBillDetailRequest req = new DescoPostpaidBillDetailRequest();
		req.billNo = billNumber;
		try {
			descoBillDetailResponse = invokeDueBillAPI(req);
		} catch (Exception ex) {
			if (ex instanceof HttpErrorResponseException) {
				if (((HttpErrorResponseException) ex).getStatus() == 401) {
					logger.info("Token expired.Login API called");
					tokenService.setTokenExpired(true);
					try {
						descoBillDetailResponse = invokeDueBillAPI(req);
					} catch (Exception e) {
						logger.info("New token generation exception: ", e);
					}
				}
			} else {
				logger.info("Exception occurred in fetchDueBillDetail(): ", ex);
			}
		}
		
		if (descoBillDetailResponse == null) {
			logger.info("Unable to collect due bill from Desco Postpaid.");
			throw new ValidationException("Unable to collect due bill from Desco Postpaid.");
		}

		if (!descoBillDetailResponse.getStatus().equalsIgnoreCase("OK")) {
			logger.info("Unable to collect due bill from Desco Postpaid.");
			throw new ValidationException("Unable to collect due bill from Desco Postpaid.");
		} else {
			billGetDetail.company = COMPANY_CODE;
			billGetDetail.consumerId = consumerId;
			GetUnpaidBillDetailResponse.Data data = new GetUnpaidBillDetailResponse.Data();
			data.billNo = descoBillDetailResponse.getBillNo();
			data.accountNo = descoBillDetailResponse.getAccountNo();
			data.meterNo = descoBillDetailResponse.getMeterNo();
			data.year = descoBillDetailResponse.getYear();
			data.month = descoBillDetailResponse.getMonth();
			data.totalAmountToBePaid = descoBillDetailResponse.getTotalAmountTobePaid();
			data.totalAmount = descoBillDetailResponse.getTotalAmount();
			data.totalVat = descoBillDetailResponse.getTotalVat();
			data.issueDate = descoBillDetailResponse.getIssueDate();
			data.dueDate = descoBillDetailResponse.getDueDate();
			data.detail = descoBillDetailResponse;
			billGetDetail.data = data;
		}
		return billGetDetail;
	}
	
	private DescoPostpaidBillInfoResponse invokeDueBillAPI(DescoPostpaidBillDetailRequest request) throws Exception {
		HttpClient client = new HttpClient(timeout);
		if (isProxyRequired) {
			client.setDefaultProxy();
		}
		DescoPostpaidBillInfoResponse response = client.postForEntity(billInfoUrl, Json.toJson(request), new HashMap<>() {
			{
				put("Content-Type", "application/json");
				put("Authorization", tokenService.getToken());
			}
		}, DescoPostpaidBillInfoResponse.class);
		return response;
	}
}