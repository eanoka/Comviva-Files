package com.grameenphone.wipro.fmfs.cbp.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.fmfs.cbp.consts.Actions;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillData;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.ClientDivision;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.ApproveRejectBillDataRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.BillDataCreationRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.BillDataFilterRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.DeleteBillDataRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.EditDataFilterRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.FilterBillRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.EditPageFilterData;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.PaginatedBillData;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.PaginatedRecordRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.generic.BaseApiResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.PaginatedBill;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.service.AccountService;
import com.grameenphone.wipro.fmfs.cbp.service.BillDataService;
import com.grameenphone.wipro.utility.common.BillDataStatus;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.orm.WhereBuilder;

@RestController
public class BillDataController {
    private final static Logger logger = LoggerFactory.getLogger(BillDataController.class);

    @Autowired
    BillDataService billDataService;

    @Autowired
    AccountService accountService;

    @Value("${temp.upload.dump.dir}")
    String dumpFileLocation;

    @JsonExcludeNestedProps("categories.companies.category")
    @RolesAllowed("Manage Bill Data")
    public EditPageFilterData filterPageData(SessionObject session, Boolean nocat) {
        User user = session.getUser();
        EditPageFilterData pageData = new EditPageFilterData();
            pageData.divisions = billDataService.getAllowedDivisions(user);
        if (nocat == null || !nocat) {
            pageData.categories = billDataService.getAllCategories();
        }
        return pageData;
    }

    @RolesAllowed("Create Payment Request")
    public BaseApiResponse queueRefreshRequest(@RequestBody BillDataFilterRequest request) {
        billDataService.createBillDetailTask(request.subAccount, request.category, request.company, request.consumerId);
        long selectableCount = billDataService.getSelectableBillCount(request.subAccount, request.category, request.company, request.consumerId);
        return new BaseApiResponse("Request is Queued for Apparently " + selectableCount + " Bill Data");
    }

    @RolesAllowed("Manage Bill Data")
    public BaseApiResponse create(@RequestBody BillDataCreationRequest request) {
        billDataService.createBillData(request.id, request.subAccount, request.company, request.consumerId, request.mobileNo, request.additionalParams, request.alias);
        return new BaseApiResponse("Billdata " + (request.id > 0 ? "Updated" : "Added") + " Successfully");
    }

    @RolesAllowed({"Manage Bill Data", "View Bill Data"})
    public PaginatedBillData allBillData(@RequestBody EditDataFilterRequest request, SessionObject session) {
        Collection<ClientDivision> clientDivisions = billDataService.getAllowedDivisions(session.getUser());
        Collection<String> statusList = Arrays.asList(BillDataStatus.VALIDATED, BillDataStatus.PENDING_FOR_REMOVAL);

        Long[] subAccountIds = request.subAccount;
        WhereBuilder<BillData, ?> billDataQuery = CrudDao.get(BillData.class).query()
        		.inif(() -> (subAccountIds != null && subAccountIds.length > 0), "clientDivision.id", subAccountIds != null ? Arrays.asList(subAccountIds): null)
        		.inif(() -> subAccountIds == null || subAccountIds.length == 0, "clientDivision", clientDivisions)
        		.eqif(() -> request.company == null && request.category != null, "company.category.id", request.category)
        		.eqif(() -> request.company != null, "company.id", request.company)
        		.eqif(() -> StringUtil.hasText(request.consumerId), "accountNo", request.consumerId)
        		.in("status", statusList);
        
        long totalCount = billDataQuery.count();
        if (totalCount <= request.offset) {
            request.offset = (long) (Math.ceil(totalCount / (double) request.totalPerPage) - 1) * request.totalPerPage;
        }
        List<BillData> records;
        if (totalCount == 0) {
            records = new ArrayList<>();
        } else {
            records = billDataQuery.findAll(request.offset, request.totalPerPage);
        }
        PaginatedBillData paginatedBillData = new PaginatedBillData();
        paginatedBillData.count = totalCount;
        paginatedBillData.offset = request.offset;
        paginatedBillData.perPage = request.totalPerPage;
        paginatedBillData.records = records;
        return paginatedBillData;
    }

    @RolesAllowed({"Validate Bill Data"})
    public PaginatedBillData billDataListForValidation(@RequestBody PaginatedRecordRequest request, SessionObject session) {
        Collection<ClientDivision> clientDivisions = billDataService.getAllowedDivisions(session.getUser());
        WhereBuilder<BillData, ?> billDataQuery = CrudDao.get(BillData.class).query().ne("status", "validated").in("clientDivision", clientDivisions);
        long totalCount = billDataQuery.count();
        if (totalCount <= request.offset) {
            request.offset = (long) (Math.ceil(totalCount / (double) request.totalPerPage) - 1) * request.totalPerPage;
        }
        List<BillData> records;
        if (totalCount == 0) {
            request.offset = 0;
            records = new ArrayList<>();
        } else {
            records = billDataQuery.findAll(request.offset, request.totalPerPage);
        }
        PaginatedBillData paginatedBillData = new PaginatedBillData();
        paginatedBillData.count = totalCount;
        paginatedBillData.offset = request.offset;
        paginatedBillData.perPage = request.totalPerPage;
        paginatedBillData.records = records;
        return paginatedBillData;
    }

    @RolesAllowed("Manage Bill Data")
    public BaseApiResponse delete(@RequestBody DeleteBillDataRequest request) {
        int deleteCount = billDataService.delete(request.ids);
        BaseApiResponse apiResponse = new BaseApiResponse();
        if (deleteCount == request.ids.length) {
            apiResponse.message = "All the " + request.ids.length + " bill data deleted successfully";
        } else {
            apiResponse.message = "All the " + request.ids.length + " bill data could not be deleted successfully";
            apiResponse.code = 5000;
        }
        return apiResponse;
    }

    @RolesAllowed("Manage Bill Data")
    public BaseApiResponse approveBills(@RequestBody ApproveRejectBillDataRequest request) {
        int deleteCount = billDataService.approveBills(request.selectedIds, request.comment);
        BaseApiResponse apiResponse = new BaseApiResponse();
        if (deleteCount == request.selectedIds.length) {
            apiResponse.message = "All the " + request.selectedIds.length + " bills approved successfully";
        } else {
            apiResponse.message = "All the " + request.selectedIds.length + " bills could not be approved successfully";
            apiResponse.code = 5000;
        }
        return apiResponse;
    }

    @RolesAllowed("Manage Bill Data")
    public BaseApiResponse rejectBills(@RequestBody ApproveRejectBillDataRequest request) {
        if (request.comment == null || request.comment.isEmpty()) {
            throw new AppRuntimeException("Comment is required for rejecting BillData.");
        }
        int deleteCount = billDataService.rejectBills(request.selectedIds, request.comment);
        BaseApiResponse apiResponse = new BaseApiResponse();
        if (deleteCount == request.selectedIds.length) {
            apiResponse.message = "All the " + request.selectedIds.length + " bill data rejected successfully";
        } else {
            apiResponse.message = "All the " + request.selectedIds.length + " bill data could not be rejected successfully";
            apiResponse.code = 5000;
        }
        return apiResponse;
    }

    @RolesAllowed("Manage Bill Data")
    public BaseApiResponse uploadBulkFile(MultipartFile bulkBillData, HttpSession session) {
        BaseApiResponse apiResponse = new BaseApiResponse();
        String dumpFileName = "BULK_BILL_DATA_" + System.currentTimeMillis() + ".xlsx";
        File dumpFile = new File(dumpFileLocation, dumpFileName);
        try {
            bulkBillData.transferTo(dumpFile);
            String successMessage = billDataService.uploadBulkData(dumpFile, session);
            apiResponse.message = successMessage;
        } catch (Throwable e) {
            if (!(e instanceof AppRuntimeException)) {
                logger.error("Couldn't process bulk bill data file", e);
            }
            apiResponse.message = "Couldn't upload bill data";
            apiResponse.code = 5001;
        }
        return apiResponse;
    }

    public void errorFileDownload(HttpSession session, HttpServletResponse response) throws IOException {
        File errorFile = (File) session.getAttribute("SESSION_TEMP_DOWNLOAD: bill_data_upload_error");
        response.getOutputStream().write(Files.readAllBytes(errorFile.toPath()));
    }

    @RolesAllowed({"Manage Bill Data", "View Bill Data"})
    public BillData getDetail(long id) {
        BillData billData = CrudDao.get(BillData.class).findOne(id);
        if (billData == null) {
            throw new AppRuntimeException("Bill data not found", 404);
        }
        return billData;
    }

    @RolesAllowed(Actions.VIEW_BILL_STATUS)
    public PaginatedBill getFilteredBills(@RequestBody FilterBillRequest request) {
        return billDataService.getFilteredBills(request);
    }
}
