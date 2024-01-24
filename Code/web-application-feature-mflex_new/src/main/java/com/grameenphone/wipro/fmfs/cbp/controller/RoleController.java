package com.grameenphone.wipro.fmfs.cbp.controller;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.fmfs.cbp.consts.Actions;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;
import com.grameenphone.wipro.fmfs.cbp.model.view.billdata.PaginatedRecordRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.generic.BaseApiResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.role.AccessiblePermissionsResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.role.PaginatedRole;
import com.grameenphone.wipro.fmfs.cbp.model.view.role.UpdatePermissionRequest;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.service.AuthService;
import com.grameenphone.wipro.fmfs.cbp.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;
import java.util.List;

@RestController
@RolesAllowed(Actions.MANAGE_ROLE)
public class RoleController {
    protected static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleService roleService;

    @Autowired
    private AuthService authService;

    @JsonExcludeNestedProps({"inheritedFrom.inheritedFrom", "inheritedFrom.actions", "inheritedFrom.client", "actions", "client"})
    public Role getRole(Long id) {
        return CrudDao.get(Role.class).findOne(id);
    }

    @PostMapping
    public BaseApiResponse createRole(@RequestBody Role role) {
        roleService.createRole(role);
        return new BaseApiResponse("Role created successfully");
    }

    public BaseApiResponse updateRole(long id, long inheritedFrom, String name) {
        roleService.updateRole(id, inheritedFrom, name);
        return new BaseApiResponse("Role updated successfully");
    }

    @JsonExcludeNestedProps({"inheritedFrom", "actions", "client"})
    public List<Role> editableRoles() {
        return roleService.editableRoles();
    }

    public PaginatedRole listableRoles(@RequestBody(required = false) PaginatedRecordRequest request) {
        return roleService.listableRoles(request == null ? 0 : request.offset, request == null ? -1 : request.totalPerPage);
    }

    public BaseApiResponse deleteRole(long id) {
        roleService.deleteRole(id);
        return new BaseApiResponse("Role deleted successfully");
    }

    @JsonExcludeNestedProps({"inheritedFrom", "actions", "client"})
    public List<Role> inheritableRoles() {
        return roleService.inheritableRoles();
    }

    public boolean isNameExist(String name, Long id) {
        return roleService.isNameExist(name, id);
    }

    @JsonExcludeNestedProps({"inheritedFrom", "actions", "client"})
    public List<Role> deletableRoles() {
        return roleService.deletableRoles();
    }

    public AccessiblePermissionsResponse getPermissions(long id) {
        AccessiblePermissionsResponse permissionsResponse = new AccessiblePermissionsResponse();
        permissionsResponse.role = roleService.getRole(id);
        if(permissionsResponse.role == null || !roleService.listableRoles(0, -1).records.contains(permissionsResponse.role)) {
            throw new AppRuntimeException("You dont have permission to access the role");
        }
        permissionsResponse.own = roleService.getOwnPermissionsOnly(permissionsResponse.role);
        permissionsResponse.cumulative = roleService.getCumulativePermissions(permissionsResponse.role);
        permissionsResponse.customActions = authService.getCustomActions();
        return permissionsResponse;
    }

    @PreAuthorize("hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).EDIT_PERMISSION) and hasAuthority(T(com.grameenphone.wipro.fmfs.cbp.consts.Actions).MANAGE_ROLE)")
    public BaseApiResponse updatePermissions(@RequestBody UpdatePermissionRequest request) {
        roleService.updatePermissions(request);
        return new BaseApiResponse("Permissions Updated Successfully");
    }
}