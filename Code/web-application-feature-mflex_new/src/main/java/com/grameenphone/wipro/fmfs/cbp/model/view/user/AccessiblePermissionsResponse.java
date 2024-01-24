package com.grameenphone.wipro.fmfs.cbp.model.view.user;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Action;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.model.view.role.AllowDeny;

import java.util.List;

@JsonExcludeNestedProps({"user.role", "user.actions", "user.clientDivisions", "user.client"})
public class AccessiblePermissionsResponse {
    public User user;
    public List<AllowDeny> cumulative;
    public List<AllowDeny> own;
    public List<Action> customActions;
}