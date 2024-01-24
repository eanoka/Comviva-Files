package com.grameenphone.wipro.fmfs.cbp.controller;

import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.view.generic.BaseApiResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.report.*;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.service.BillDataService;
import com.grameenphone.wipro.fmfs.cbp.service.PaymentRequestService;
import com.grameenphone.wipro.fmfs.cbp.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@RestController
public class ReportController {
    @Autowired
    BillDataService billDataService;

    @Autowired
    ReportService reportService;

    @Autowired
    PaymentRequestService paymentRequestService;

    public FilterData getFilterPageData(SessionObject session) {
        FilterData filterData = new FilterData();
        if(session.IS_GP) {
            filterData.clients = CrudDao.get(Client.class).query().findAll();
        }
        filterData.categories = billDataService.getAllCategories();
        return filterData;
    }

    public FilterVatData getFilterVatPageData(SessionObject session) {
    	FilterVatData filterData = new FilterVatData();
    	List<RequestModal> requestModals = paymentRequestService.getAllCompletedRequests();
    	filterData.requests = requestModals;
        return filterData;
    }
    
    public PaginatedReportData getDetailReportData(SessionObject object, @RequestBody DetailReportRequest request) {
        if(object.IS_GP) {
            if(request.account == null) {
                throw new AppRuntimeException("Account selection is mandatory", 403);
            }
            if(request.subAccount != null && request.subAccount.length > 0) {
                throw new AppRuntimeException("You can not select sub account for other account", 403);
            }
        } else {
            if (request.account != null) {
                throw new AppRuntimeException("You have no permission to view reports for other Account", 403);
            }
        }
        return reportService.getDetailReportData(request, false);
    }
    
    public PaginatedReportData getVatReportData(SessionObject object, @RequestBody VatReportRequest request) {
       
		if (request.requestId == null) {
			throw new AppRuntimeException("Request Id selection is mandatory", 403);
		}
        return reportService.getVatReportData(request, true);
    }

    public void downloadReportData(SessionObject object, Long account, long[] subAccount, Long category, Long company, @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss") Date start, @DateTimeFormat(pattern = "dd/MM/yyyy HH:mm:ss") Date end, String type, String accNo, HttpServletResponse response) throws IOException {
        DetailReportRequest request = new DetailReportRequest();
        request.account = account;
        request.subAccount = subAccount;
        request.category = category;
        request.company = company;
        request.start = start;
        request.end = end;
        request.type = type;
        request.accNo = accNo;

        if(object.IS_GP) {
            if(request.account == null) {
                throw new AppRuntimeException("Account selection is mandatory", 403);
            }
            if(request.subAccount != null && request.subAccount.length > 0) {
                throw new AppRuntimeException("You can not select sub account for other account", 403);
            }
        } else {
            if (request.account != null) {
                throw new AppRuntimeException("You have no permission to view reports for other Account", 403);
            }
        }
        reportService.downloadAsXls(request, response.getOutputStream());
    }

    public BaseApiResponse sendPrepaidTokenSMS(Long tid) {
        String error = reportService.sendTokenSMS(tid);
        if(error != null) {
            BaseApiResponse apiResponse = new BaseApiResponse(error);
            apiResponse.code = 500;
            return apiResponse;
        }
        return new BaseApiResponse("SMS has been transferred");
    }
}
