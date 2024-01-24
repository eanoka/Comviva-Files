package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

public class BalanceDetail {

    private String balance;
    private String last_refill_on;
    private String last_refill_amount;

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getLast_refill_on() {
        return last_refill_on;
    }

    public void setLast_refill_on(String last_refill_on) {
        this.last_refill_on = last_refill_on;
    }

    public String getLast_refill_amount() {
        return last_refill_amount;
    }

    public void setLast_refill_amount(String last_refill_amount) {
        this.last_refill_amount = last_refill_amount;
    }

}
