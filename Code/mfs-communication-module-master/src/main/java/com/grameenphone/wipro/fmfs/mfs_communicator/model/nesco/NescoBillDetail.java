package com.grameenphone.wipro.fmfs.mfs_communicator.model.nesco;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class NescoBillDetail {

    private String bill_number;
    private int account_number;
    @JsonFormat(pattern = "dd-MMM-yy", timezone = "Asia/Dhaka")
    private Date due_date;
    private String name;
    private String street_address;
    private String year;
    private String month;
    @JsonFormat(pattern = "dd-MMM-yy", timezone = "Asia/Dhaka")
    private Date issue_date;
    private String problem_sequence_string;
    private String zone_code;
    private int bill_amount;
    private int vat_amount;
    private int lpc_amount;
    private int total_amount;
    private int total_amount_with_lpc;

    public String getBill_number() {
        return bill_number;
    }

    public void setBill_number(String bill_number) {
        this.bill_number = bill_number;
    }

    public int getTotal_amount() {
        return total_amount;
    }

    public void setTotal_amount(int total_amount) {
        this.total_amount = total_amount;
    }

    public int getBill_amount() {
        return bill_amount;
    }

    public void setBill_amount(int bill_amount) {
        this.bill_amount = bill_amount;
    }

    public Date getDue_date() {
        return due_date;
    }

    public void setDue_date(Date due_date) {
        this.due_date = due_date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public int getLpc_amount() {
        return lpc_amount;
    }

    public void setLpc_amount(int lpc_amount) {
        this.lpc_amount = lpc_amount;
    }

    public int getAccount_number() {
        return account_number;
    }

    public void setAccount_number(int account_number) {
        this.account_number = account_number;
    }

    public String getStreet_address() {
        return street_address;
    }

    public void setStreet_address(String street_address) {
        this.street_address = street_address;
    }

    public Date getIssue_date() {
        return issue_date;
    }

    public void setIssue_date(Date issue_date) {
        this.issue_date = issue_date;
    }

    public String getProblem_sequence_string() {
        return problem_sequence_string;
    }

    public void setProblem_sequence_string(String problem_sequence_string) {
        this.problem_sequence_string = problem_sequence_string;
    }

    public String getZone_code() {
        return zone_code;
    }

    public void setZone_code(String zone_code) {
        this.zone_code = zone_code;
    }

    public int getTotal_amount_with_lpc() {
        return total_amount_with_lpc;
    }

    public void setTotal_amount_with_lpc(int total_amount_with_lpc) {
        this.total_amount_with_lpc = total_amount_with_lpc;
    }

    public int getVat_amount() {
        return vat_amount;
    }

    public void setVat_amount(int vat_amount) {
        this.vat_amount = vat_amount;
    }

}
