package com.grameenphone.wipro.fmfs.mfs_communicator.model.reb;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class RebGetBillResponseData {
    public String BILL_NO;
    public String BOOK_NO;
    public String SMS_AC_NO;
    public String BILL_MONTH;
    public String BILL_YEAR;
    @JsonFormat(pattern = "dd/MM/yyyy")
    public Date ISSUE_DATE;
    @JsonFormat(pattern = "dd/MM/yyyy")
    public Date DUE_DATE;
    public int DUE_AMOUNT;
    public String DUE_TYPE = "DUE_WITHOUT_LPC";
    @JsonFormat(pattern = "dd/MM/yyyy")
    public Date LPC_DATE;
    public String PAID_STATUS;
    public String PBS_CODE;
    public String PBS_NAME_B;
    public String PBS_NAME_E;
}