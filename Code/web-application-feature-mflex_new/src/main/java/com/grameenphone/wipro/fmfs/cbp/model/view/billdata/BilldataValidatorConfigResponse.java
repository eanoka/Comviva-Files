package com.grameenphone.wipro.fmfs.cbp.model.view.billdata;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;

import java.util.List;

@JsonExcludeNestedProps({"allowedUsers.role","allowedUsers.client","allowedUsers.clientDivisions","allowedUsers.actions","allowedRoles.inheritedFrom","allowedRoles.client","allowedRoles.actions","allUsers.role","allUsers.client","allUsers.clientDivisions","allUsers.actions","allRoles.inheritedFrom","allRoles.client","allRoles.actions"})
public class BilldataValidatorConfigResponse {
    public boolean isEnabled;
    public List<User> allowedUsers;
    public List<Role> allowedRoles;
    public List<User> allUsers;
    public List<Role> allRoles;
}