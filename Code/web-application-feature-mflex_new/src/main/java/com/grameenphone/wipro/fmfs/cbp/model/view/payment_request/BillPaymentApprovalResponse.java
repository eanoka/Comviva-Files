package com.grameenphone.wipro.fmfs.cbp.model.view.payment_request;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.WorkflowHop;

import java.util.List;

@JsonExcludeNestedProps({"firstLevelApprover.users.role","firstLevelApprover.users.client","firstLevelApprover.users.clientDivisions","firstLevelApprover.users.actions","firstLevelApprover.roles.inheritedFrom","firstLevelApprover.roles.client","firstLevelApprover.roles.actions","hops.users.role","hops.users.client","hops.users.clientDivisions","hops.users.actions","hops.roles.inheritedFrom","hops.roles.client","hops.roles.actions","users.role","users.client","users.clientDivisions","users.actions","roles.inheritedFrom","roles.client","roles.actions"})
public class BillPaymentApprovalResponse {
    public static class BillPaymentApprovalHop {
        public WorkflowHop hop;
        public List<User> users;
        public List<Role> roles;
    }

    public List<BillPaymentApprovalHop> hops;
    public List<User> users;
    public List<Role> roles;
    public BillPaymentApprovalHop firstLevelApprover = new BillPaymentApprovalHop();
    public BillPaymentApprovalHop lastLevelApprover = new BillPaymentApprovalHop();
}