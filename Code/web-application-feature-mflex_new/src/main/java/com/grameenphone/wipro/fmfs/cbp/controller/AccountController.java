package com.grameenphone.wipro.fmfs.cbp.controller;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.constants.Events;
import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.fmfs.cbp.consts.Actions;
import com.grameenphone.wipro.fmfs.cbp.model.api.comm_module.BalanceResponse.Balance;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.view.account.CreateRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.BilldataValidatorConfigRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.BilldataValidatorConfigResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.generic.BaseApiResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.BillPaymentApprovalRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.BillPaymentApprovalResponse;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.service.AccountService;
import com.grameenphone.wipro.fmfs.cbp.service.AuthService;
import com.grameenphone.wipro.fmfs.cbp.service.PaymentRequestService;
import com.grameenphone.wipro.utility.common.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;
import java.io.IOException;
import java.util.List;

@RestController
public class AccountController {
    @Autowired
    private AccountService accountService;
    @Autowired
    private AuthService authService;
    @Autowired
    private PaymentRequestService paymentRequestService;

    public boolean isNameExist(String name) {
        return accountService.isNameExist(name);
    }

    public boolean isMsisdnExist(long msisdn) {
        return accountService.isMsisdnExist(msisdn);
    }

    @RolesAllowed(Actions.MANAGE_ACCOUNT)
    public BaseApiResponse createAccount(@RequestBody CreateRequest request) {
        accountService.createAccount(request.name, request.mobileNo, request.address1, request.address2, request.description);
        return new BaseApiResponse("Account Created Successfully");
    }

    @JsonExcludeNestedProps("clientDivisions")
    public List<Client> getAllActive(SessionObject session) {
        if(!session.IS_GP) {
            throw new AppRuntimeException("You are not permitted to collect all account list", 403);
        }
        return accountService.getAllActive();
    }

    @JsonExcludeNestedProps("clientDivisions")
    public List<Client> getAll(SessionObject session) {
        if(!session.IS_GP) {
            throw new AppRuntimeException("You are not permitted to collect all account list", 403);
        }
        return CrudDao.get(Client.class).query().findAll();
    }

    public boolean isBarred(long accountId) {
        return !(Boolean)CrudDao.get(Client.class).query().eq("id", accountId).selectOne("active");
    }

    @RolesAllowed(Actions.MANAGE_ACCOUNT)
    public BaseApiResponse barUnbarAccount(long account, boolean action) throws HttpErrorResponseException {
        accountService.barUnbarAccount(account, action);
        return new BaseApiResponse("Operation Performed Successfully");
    }

    @RolesAllowed(Actions.CHECK_BALANCE)
    public Balance checkBalance(Long account) throws IOException {
        return accountService.checkBalance(account);
    }

    @PreAuthorize("hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).MANAGE_ACCOUNT) and hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).EDIT_PERMISSION)")
    public BaseApiResponse updateAccountValidators(@RequestBody BilldataValidatorConfigRequest request) {
		accountService.updateAccountValidators(request);
        return new BaseApiResponse("Client configuration is Updated Successfully");
    }
    
	@RolesAllowed(Actions.MANAGE_ACCOUNT)
    public BilldataValidatorConfigResponse getBilldataValidatorConfig() {
        BilldataValidatorConfigResponse response = new BilldataValidatorConfigResponse();
        response.isEnabled = accountService.isBilldataValidationEnabled();
        response.allowedRoles = authService.getPermittedRolesForAction(Actions.VALIDATE_BILL_DATA);
        response.allowedUsers = authService.getPermittedUsersForAction(Actions.VALIDATE_BILL_DATA);
        response.allRoles = authService.getPermittedRolesForAction(null);
        response.allUsers = authService.getPermittedUsersForAction(null);
		return response;
    }

	@RolesAllowed(Actions.MANAGE_ACCOUNT)
    public BillPaymentApprovalResponse getAccountPaymentApproverConfig() {
        BillPaymentApprovalResponse approval = new BillPaymentApprovalResponse();
        approval.firstLevelApprover.users = authService.getPermittedUsersForAction(Actions.APPROVE_PAYMENT);
        approval.firstLevelApprover.roles = authService.getPermittedRolesForAction(Actions.APPROVE_PAYMENT);
        approval.lastLevelApprover.users = authService.getPermittedUsersForAction(Actions.INITIATE_PAYMENT);
        approval.lastLevelApprover.roles = authService.getPermittedRolesForAction(Actions.INITIATE_PAYMENT);
        approval.hops = authService.getCustomWorkflowHops();
        approval.users = authService.getPermittedUsersForAction(null);
        approval.roles = authService.getPermittedRolesForAction(null);
        return approval;
    }

    @PreAuthorize("hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).MANAGE_ACCOUNT) and hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).EDIT_PERMISSION)")
    public BaseApiResponse saveAccountPaymentApproverConfig(@RequestBody BillPaymentApprovalRequest approval, SessionObject session) {
        paymentRequestService.updatePaymentApprovers(approval.firstLevelApprover, approval.lastLevelApprover);
        paymentRequestService.updatePaymentApproversHops(approval.hops);
        Event.fire(Events.HOP_CONFIG_CHANGE, session.getUser().getClient().getId());
        return new BaseApiResponse();
    }
}