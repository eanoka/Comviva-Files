package com.grameenphone.wipro.fmfs.cbp.model.view.payment_request;

import java.util.Date;
import java.util.List;

public class PaginatedPRRequest {
    public long offset = 0;
    public int totalPerPage = 10;
    public Date start;
    public Date end;
    public Long account;
    public List<Long> subAccount;
    public Long category;
    public Long company;
    public String accno;
    public boolean my = false;
}