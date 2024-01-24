package com.grameenphone.wipro.fmfs.cbp.controller;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.fmfs.cbp.consts.Actions;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.PaginatedRecordRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.generic.BaseApiResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.role.UpdatePermissionRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.user.AccessiblePermissionsResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.user.CreateFormInitializationDataResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.user.PaginatedUser;
import com.grameenphone.wipro.fmfs.cbp.model.view.user.UserCreationRequest;
import com.grameenphone.wipro.fmfs.cbp.service.AuthService;
import com.grameenphone.wipro.fmfs.cbp.service.UserService;
import com.grameenphone.wipro.utility.common.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
public class UserController {
    @Autowired
    UserService userService;

    @Autowired
    AuthService authService;

    @JsonExcludeNestedProps({"role.inheritedFrom", "role.actions", "role.client", "client", "clientDivisions", "actions"})
    @RolesAllowed({Actions.EDIT_PERMISSION})
    public List<User> permissionModifiableUsers() {
        return userService.permissionModifiableUsers();
    }

    @JsonExcludeNestedProps({"role.inheritedFrom", "role.actions", "role.client", "client.clientDivisions", "clientDivisions", "actions"})
    @RolesAllowed({Actions.BAR_USER})
    public List<User> barUnbarrableUsers() {
        return userService.permittedUsersFor(Actions.BAR_OTHER_ACCOUNT_USER);
    }

    @JsonExcludeNestedProps({"role.inheritedFrom", "role.actions", "role.client", "client.clientDivisions", "clientDivisions", "actions"})
    @RolesAllowed({Actions.DELETE_USER})
    public List<User> deletableUsers() {
        return userService.permittedUsersFor(Actions.DELETE_OTHER_ACCOUNT_USER);
    }

    @JsonExcludeNestedProps({"role.inheritedFrom", "role.actions", "role.client", "client.clientDivisions", "clientDivisions", "actions"})
    @RolesAllowed({Actions.EDIT_USER})
    public List<User> editableUsers() {
        return userService.permittedUsersFor(Actions.EDIT_OTHER_ACCOUNT_USER);
    }

    @RolesAllowed({Actions.LIST_USERS})
    public PaginatedUser listableUsers(@RequestBody PaginatedRecordRequest request) {
        return userService.listableUsers(request.offset, request.totalPerPage);
    }

    @RolesAllowed({Actions.LIST_USERS})
    public void downloadAsXlsx(HttpServletResponse response) throws IOException {
        userService.downloadAsXls(response.getOutputStream());
    }

    @PreAuthorize("hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).EDIT_PERMISSION) and hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).EDIT_USER)")
    public AccessiblePermissionsResponse getPermissions(long id) {
        AccessiblePermissionsResponse permissionsResponse = new AccessiblePermissionsResponse();
        permissionsResponse.user = userService.getUser(id);
        if(permissionsResponse.user == null || !userService.permissionModifiableUsers().contains(permissionsResponse.user)) {
            throw new AppRuntimeException("You dont have permission to edit permission for this user");
        }
        permissionsResponse.own = userService.getOwnPermissionsOnly(permissionsResponse.user);
        permissionsResponse.cumulative = userService.getCumulativePermissions(permissionsResponse.user);
        permissionsResponse.customActions = authService.getCustomActions();
        return permissionsResponse;
    }

    @PreAuthorize("hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).EDIT_PERMISSION) and hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).EDIT_USER)")
    public BaseApiResponse updatePermissions(@RequestBody UpdatePermissionRequest request) {
        userService.updatePermissions(request);
        return new BaseApiResponse("Permissions Updated Successfully");
    }

    @RolesAllowed(Actions.BAR_USER)
    public BaseApiResponse toggleActive(Long id, String remarks) {
        boolean isBarred = userService.toggleActive(id, remarks);
        return new BaseApiResponse("User successfully " + (isBarred ? "barred" : "unbarred"));
    }

    @RolesAllowed(Actions.DELETE_USER)
    public BaseApiResponse delete(Long id, String remarks) {
        userService.delete(id, remarks);
        return new BaseApiResponse("User successfully deleted");
    }

    @RolesAllowed(Actions.CREATE_USER)
    public BaseApiResponse create(@RequestBody UserCreationRequest request) {
        userService.update(request);
        return new BaseApiResponse("User successfully created");
    }

    @RolesAllowed(Actions.EDIT_USER)
    public BaseApiResponse update(@RequestBody UserCreationRequest request) {
        userService.update(request);
        return new BaseApiResponse("User successfully updated");
    }

    @PreAuthorize("(#id == null and hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).CREATE_USER)) or (#id != null and hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).EDIT_USER))")
    public CreateFormInitializationDataResponse loadCreateFormEntities(Long id) {
        return userService.getCreateFormInitializationData(id);
    }

    public boolean isEmailExist(Long id, String email) {
        return userService.isExist(id, "email", email);
    }

    public boolean isAdidExist(Long id, String adid) {
        return userService.isExist(id, "adid", adid);
    }

    public boolean isMsisdnExist(Long id, String msisdn) {
        msisdn = StringUtil.sanitizeMsisdn(msisdn);
        if(msisdn == null) {
            return false;
        }
        return userService.isExist(id, "msisdn", msisdn);
    }
}