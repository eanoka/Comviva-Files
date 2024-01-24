package com.grameenphone.wipro.fmfs.cbp.model.view.billdata;

import java.util.List;

public class FilterBillRequest {
    public Long category;
    public Long company;
    public List<Long> subAccount;
    public String accno;
    public Long account;
    public long offset = 0;
    public int totalPerPage = 10;
}