package com.grameenphone.wipro.fmfs.cbp.model.view.billdata;

import java.util.ArrayList;
import java.util.List;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;

@JsonExcludeNestedProps({"users.role","users.client","users.clientDivisions","users.actions","roles.inheritedFrom","roles.client","roles.actions"})
public class BilldataValidatorConfig {
    public String key;
    public boolean isEnabled;
    public List<Role> roles;
    public List<User> users;
    
    public List<String> boundRoles = new ArrayList<>();
    public List<String> boundUsers = new ArrayList<>();
}