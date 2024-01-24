package com.grameenphone.wipro.fmfs.cbp.model.view.billdata;

import com.grameenphone.wipro.utility.excel.SheetColumn;

@SheetColumn(width = 20)
public class BulkBillDataExcelProcessReport {
    @SheetColumn(label = "REF_SL_NO")
    public int reference;

    @SheetColumn(label = "SUB_ACCOUNT")
    public String subAccountName;

    @SheetColumn(label = "BILL_CATEGORY")
    public String categoryName;

    @SheetColumn(label = "BILL_COMPANY")
    public String companyName;

    @SheetColumn(label = "ACCOUNT_NO")
    public String accountNo;

    @SheetColumn(label = "MSISDN")
    public String msisdn;

    @SheetColumn(label = "ERROR_DESCRIPTION")
    public String error;
}