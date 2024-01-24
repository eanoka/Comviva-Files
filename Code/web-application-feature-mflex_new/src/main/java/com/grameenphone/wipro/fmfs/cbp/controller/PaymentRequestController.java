package com.grameenphone.wipro.fmfs.cbp.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.RolesAllowed;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.grameenphone.wipro.annot.PaylodLoggerInterceptor;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.fmfs.cbp.consts.Actions;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Bill;
import com.grameenphone.wipro.fmfs.cbp.enums.WorkflowHops;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillDetailTask;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.PaymentRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.PaginatedRecordRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.generic.BaseApiResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.ApprovePRRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.InitiatePRRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedBill;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedBillDetailTask;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedBillsInPRRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedDueBillFilterRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedPRRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedPaymentRequestDetail;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.RequestSummary;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.service.PaymentRequestService;
import com.grameenphone.wipro.utility.common.JsonPayloadPinMasker;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.orm.WhereBuilder;

@RestController
@PaylodLoggerInterceptor(pattern = "/api/paymentRequest/initiatePayments", interceptor = JsonPayloadPinMasker.class)
public class PaymentRequestController {
	private final static Logger logger = LoggerFactory.getLogger(BillDataController.class);

	@Autowired
	PaymentRequestService paymentRequestService;

	@RolesAllowed(Actions.CREATE_PAYMENT)
	public PaginatedBill filteredBillDataList(@RequestBody PaginatedDueBillFilterRequest request) {
		return paymentRequestService.getSelectableUnpaidBills(request.subAccount, request.category, request.company,
				request.consumerId, request.offset, request.totalPerPage);
	}

	@RolesAllowed(Actions.CREATE_PAYMENT)
	public BaseApiResponse initiateRequest(@RequestParam String billsJson, @RequestParam(required = false) MultipartFile attachment) throws IOException {
		Map<Long, Map<String,Object>> map = Json.fromJson(billsJson, new TypeReference<>() {});
		if (null != attachment && !attachment.isEmpty()) {
			if (attachment.getSize() / 1024 / 1024 > 5) {
				logger.error("Uploaded file size is greater than 5 MB");
				BaseApiResponse apiResponse = new BaseApiResponse();
				apiResponse.message = "Uploaded file size is greater than 5 MB";
				apiResponse.code = 5002;
				return apiResponse;  
			}
		} 
		PaymentRequest paymentRequest = paymentRequestService.createPaymentRequest(map, attachment);
		paymentRequestService.sendCreationNotification(paymentRequest, attachment);
		return new BaseApiResponse("Your request with id PR" + String.format("%08x", paymentRequest.getId()).toUpperCase()
				+ " is created and now it's waiting for approval");		
	}

	@RolesAllowed({"View Bill Data" })
	public PaginatedBillDetailTask allBillCollectionRequest(@RequestBody PaginatedRecordRequest request, SessionObject session) {
        WhereBuilder<BillDetailTask, ?> billDetailTask = CrudDao.get(BillDetailTask.class).query();
        long totalCount = billDetailTask.count();
        if (totalCount <= request.offset) {
            request.offset = (long) (Math.ceil(totalCount / (double) request.totalPerPage) - 1) * request.totalPerPage;
        }
        List<BillDetailTask> records;
        if (totalCount == 0) {
            request.offset = 0;
            records = new ArrayList<>();
        } else {
            records = billDetailTask.findAll(request.offset, request.totalPerPage, "id", "DESC");
        }
        PaginatedBillDetailTask PaginatedBillDetailTask = new PaginatedBillDetailTask();
        PaginatedBillDetailTask.count = totalCount;
        PaginatedBillDetailTask.offset = request.offset;
        PaginatedBillDetailTask.perPage = request.totalPerPage;
		PaginatedBillDetailTask.records = records;
        return PaginatedBillDetailTask;
	}

	public int getApprovalWaitingCount() {
		return paymentRequestService.getApprovalWaitingCount();
	}

	public List<RequestSummary> getApprovalWaitingRequests() {
		return paymentRequestService.getApprovalWaitingRequests();
	}

	@RolesAllowed(Actions.INITIATE_PAYMENT)
	public int getApprovedCount() { 
		return paymentRequestService.getApprovedCount();
	}

	@RolesAllowed(Actions.INITIATE_PAYMENT)
	public List<RequestSummary> getApprovedRequests() {
		return paymentRequestService.getApprovedRequests();
	}

	@RolesAllowed({ Actions.VIEW_BILL_STATUS })
	public PaginatedBill getPaginatedBillsForRequest(@RequestBody PaginatedBillsInPRRequest request) {
		return paymentRequestService.getPaginatedBills(request.requestId, request.offset, request.totalPerPage);
	}

	@RolesAllowed({ Actions.CREATE_PAYMENT})
	public List<Bill> getPaybleBillsForRequest(@RequestBody PaginatedBillsInPRRequest request) {
		return paymentRequestService.getPaybleBillsForRequest(request.requestId);
	}

	public BaseApiResponse approvePayments(@RequestBody ApprovePRRequest request) throws HttpErrorResponseException {
		List<PaymentRequest> paymentRequests = paymentRequestService.approvePayments(request.comment, request.ids);
		paymentRequests.forEach(pr -> {
			if(pr.getLastHop().getWorkflowHop().getCode().equals(WorkflowHops.WPI)) {
			paymentRequestService.sendApprovedNotification(pr, pr.getAttachment());
			}else {
				paymentRequestService.sendLevelWiseApprovedNotification(pr, pr.getAttachment());
			}
		});
		return new BaseApiResponse("Selected requests are now ready for payment");
	}

	public BaseApiResponse rejectApprovals(@RequestBody ApprovePRRequest request) throws HttpErrorResponseException {
		List<PaymentRequest> paymentRequests = paymentRequestService.rejectApprovals(request.comment, request.ids);
		if (request.billIdsToReject.length != 0 && request.ids.length == 1) {
			PaymentRequest pr = paymentRequests.get(0);
			paymentRequestService.sendPartialPRRejectionNotification(pr, request.billIdsToReject, pr.getAttachment());
		} else {
		paymentRequests.forEach(pr -> {
			paymentRequestService.sendRejectionNotification(pr, pr.getAttachment());
		});
		}
		return new BaseApiResponse("Selected requests are marked as rejected");
	}

	@RolesAllowed(Actions.INITIATE_PAYMENT)
	public BaseApiResponse initiatePayments(@RequestBody InitiatePRRequest request)
			throws IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException,
			IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
		List<PaymentRequest> paymentRequests = paymentRequestService.initiatePayments(request.pin, request.comment,
				request.ids);
		paymentRequests.forEach(pr -> {
			paymentRequestService.sendInitiatedNotification(pr, pr.getAttachment());
		});
		return new BaseApiResponse("Selected requests are now scheduled for execution");
	}

	@RolesAllowed(Actions.VIEW_REQUEST_STATUS)
	public PaginatedPaymentRequestDetail getFilteredRequests(@RequestBody PaginatedPRRequest request) {
		return paymentRequestService.getFilteredRequests(request);
	}

	@RolesAllowed({ Actions.VIEW_BILL_STATUS })
	public void downloadAsXls(Long id, ServletOutputStream outputStream) throws IOException {
		paymentRequestService.downloadAsXls(id, outputStream);
	}

	public void errorFileUpload(HttpSession session, HttpServletResponse response) throws IOException {
		File errorFile = (File) session.getAttribute("SESSION_TEMP_DOWNLOAD: payment_request_upload_error");
		response.getOutputStream().write(Files.readAllBytes(errorFile.toPath()));
	}

	@RolesAllowed(Actions.VIEW_REQUEST_STATUS)
	public void downloadPaymentRequestDocument(Long id, ServletOutputStream outputStream) throws IOException {
		paymentRequestService.downloadPaymentRequestFile(id, outputStream);
	}
}
