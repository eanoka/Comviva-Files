package com.grameenphone.wipro.fmfs.cbp.model.view.report;

import java.util.Date;

public class DetailReportRequest {
    public Long account;
    public long[] subAccount;
    public Long category;
    public Long company;
    public Date start;
    public Date end;
    public long offset;
    public int perPage = 10;
    public String type;
    public String accNo;
}