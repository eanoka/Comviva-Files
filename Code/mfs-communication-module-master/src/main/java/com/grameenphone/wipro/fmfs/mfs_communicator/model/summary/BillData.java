package com.grameenphone.wipro.fmfs.mfs_communicator.model.summary;

import java.util.Date;

public class BillData {
    public String companyCode;
    public String accountNumber;
    public String billNumber;
    public String month;
    public Date dueDate;
    public int amount;
    public String status;
    public Double vat;

    public BillData(String companyCode, String accountNumber, String billNumber, String month, Date dueDate, int amount, String status, Double vat) {
        this.companyCode = companyCode;
        this.accountNumber = accountNumber;
        this.billNumber = billNumber;
        this.month = month;
        this.dueDate = dueDate;
        this.amount = amount;
        this.status = status;
        this.vat = vat;
    }
}