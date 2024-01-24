package com.grameenphone.wipro.fmfs.mfs_communicator.model.nesco;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class BillInfoDetail {
    private String customer_name;
    private String account_number;
    private String bill_number;
    private String issue_date;
    private String bill_year;
    private String bill_month;
    @JsonFormat(pattern = "dd-MMM-yy", timezone= "Asia/Dhaka")
    private Date due_date;
    private String zone_code;
    private String bill_amount;
    private String vat_amount;
    private String stamp_amount;
    private String lpc_amount;
    private int total_amount;

    public String getCustomer_name() {
        return customer_name;
    }

    public void setCustomer_name(String customer_name) {
        this.customer_name = customer_name;
    }

    public String getAccount_number() {
        return account_number;
    }

    public void setAccount_number(String account_number) {
        this.account_number = account_number;
    }

    public String getBill_number() {
        return bill_number;
    }

    public void setBill_number(String bill_number) {
        this.bill_number = bill_number;
    }

    public String getIssue_date() {
        return issue_date;
    }

    public void setIssue_date(String issue_date) {
        this.issue_date = issue_date;
    }

    public String getBill_year() {
        return bill_year;
    }

    public void setBill_year(String bill_year) {
        this.bill_year = bill_year;
    }

    public String getBill_month() {
        return bill_month;
    }

    public void setBill_month(String bill_month) {
        this.bill_month = bill_month;
    }

    public Date getDue_date() {
        return due_date;
    }

    public void setDue_date(Date due_date) {
        this.due_date = due_date;
    }

    public String getZone_code() {
        return zone_code;
    }

    public void setZone_code(String zone_code) {
        this.zone_code = zone_code;
    }

    public String getBill_amount() {
        return bill_amount;
    }

    public void setBill_amount(String bill_amount) {
        this.bill_amount = bill_amount;
    }

    public String getVat_amount() {
        return vat_amount;
    }

    public void setVat_amount(String vat_amount) {
        this.vat_amount = vat_amount;
    }

    public String getStamp_amount() {
        return stamp_amount;
    }

    public void setStamp_amount(String stamp_amount) {
        this.stamp_amount = stamp_amount;
    }

    public String getLpc_amount() {
        return lpc_amount;
    }

    public void setLpc_amount(String lpc_amount) {
        this.lpc_amount = lpc_amount;
    }

    public int getTotal_amount() {
        return total_amount;
    }

    public void setTotal_amount(int total_amount) {
        this.total_amount = total_amount;
    }
}