package com.grameenphone.wipro.fmfs.cbp.model.view.payment_request;

public class PaginatedDueBillFilterRequest {
    public long[] subAccount;
    public long category;
    public long company;
    public String consumerId;
    public long offset = 0;
    public int totalPerPage = 10;
}