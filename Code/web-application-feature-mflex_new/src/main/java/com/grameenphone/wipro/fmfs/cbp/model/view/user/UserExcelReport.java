package com.grameenphone.wipro.fmfs.cbp.model.view.user;

import com.grameenphone.wipro.utility.excel.SheetColumn;

public class UserExcelReport {
    @SheetColumn(label = "ROLE", width = 20)
    public String roleName;

    @SheetColumn(label = "ACCOUNT_NAME", width = 30)
    public String accountName;

    @SheetColumn(label = "NAME", width = 30)
    public String userName;

    @SheetColumn(label = "EMAIL", width = 30)
    public String email;

    @SheetColumn(label = "ADDRESS", width = 40)
    public String address;

    @SheetColumn(label = "STATUS", width = 10)
    public String status;

    public UserExcelReport(String roleName, String accountName, String userName, String email, String address, String status) {
        this.roleName = roleName;
        this.accountName = accountName;
        this.userName = userName;
        this.email = email;
        this.address = address;
        this.status = status;
    }
}