package com.grameenphone.wipro.fmfs.cbp.model.view.payment_request;

import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.WorkflowHop;

import java.util.List;

public class BillPaymentApprovalRequest {
    public static class BillPaymentApprovalHop {
        public WorkflowHop hop;
        public List<Long> boundUsers;
        public List<Long> boundRoles;
    }

    public List<BillPaymentApprovalHop> hops;
    public BillPaymentApprovalHop firstLevelApprover = new BillPaymentApprovalHop();
    public BillPaymentApprovalHop lastLevelApprover = new BillPaymentApprovalHop();
}