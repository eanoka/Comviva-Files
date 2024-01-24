package com.grameenphone.wipro.fmfs.cbp.model.view.role;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Action;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;

import java.util.List;

@JsonExcludeNestedProps({"role.inheritedFrom", "role.actions", "role.client"})
public class AccessiblePermissionsResponse {
    public Role role;
    public List<AllowDeny> cumulative;
    public List<AllowDeny> own;
    public List<Action> customActions;
}