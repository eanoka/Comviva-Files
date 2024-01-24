package com.grameenphone.wipro.fmfs.cbp.model.view.billdata;

import com.grameenphone.wipro.fmfs.cbp.enums.BillStatus;
import com.grameenphone.wipro.utility.excel.SheetColumn;

import java.util.Date;

public class BillExcelReport {
    @SheetColumn(label = "ACCOUNT")
    public String accountName;

    @SheetColumn(label = "SUB_ACCOUNT", width = 25)
    public String subAccountName;

    @SheetColumn(label = "BILL_CATEGORY", width = 25)
    public String categoryName;

    @SheetColumn(label = "BILL_COMPANY", width = 25)
    public String companyName;

    @SheetColumn(label = "ACCOUNT_NO", width = 25)
    public String accountNo;

    @SheetColumn(label = "GPAY_TRANSACTION_ID", width = 25)
    public String mfsTxnId;

    @SheetColumn(label = "BILL_AMOUNT", width = 15)
    public Double billAmount;

    @SheetColumn(label = "VAT", width = 10)
    public Double vat;

    @SheetColumn(label = "TOTAL_DUE", width = 15)
    public Double totalDue;

    @SheetColumn(label = "SERVICE_CHARGE", width = 15)
    public Double serviceCharge;

    @SheetColumn(label = "TOTAL_PAYABLE", width = 15)
    public Double totalPayable;

    @SheetColumn(label = "DUE_DATE", width = 20)
    public Date dueDate;

    @SheetColumn(label = "STATUS")
    public BillStatus status;

    public BillExcelReport(String accountName, String subAccountName, String categoryName, String companyName, String accountNo, String mfsTxnId, Double billAmount, Double vat, Double totalDue, Double serviceCharge, Double totalPayable, Date dueDate, BillStatus status) {
        this.accountName = accountName;
        this.subAccountName = subAccountName;
        this.categoryName = categoryName;
        this.companyName = companyName;
        this.accountNo = accountNo;
        this.mfsTxnId = mfsTxnId;
        this.billAmount = billAmount;
        this.vat = vat;
        this.totalDue = totalDue;
        this.serviceCharge = serviceCharge;
        this.totalPayable = totalPayable;
        this.dueDate = dueDate;
        this.status = status;
    }
}