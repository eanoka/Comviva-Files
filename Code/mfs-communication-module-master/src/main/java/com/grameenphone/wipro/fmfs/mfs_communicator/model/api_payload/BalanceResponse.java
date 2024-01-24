package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.BalanceResponse.Balance;

import java.util.Date;

public class BalanceResponse extends BaseResponse<Balance> {
    public static class Balance {
        public double balance;
        public String msisdn;
        public Date last_refill_on;
        public int last_refill_amount;

        public Balance() {
        }

        public Balance(double balance, Date last_refill_on, int last_refill_amount, String msisdn) {
            this.balance = balance;
            this.last_refill_on = last_refill_on;
            this.last_refill_amount = last_refill_amount;
            this.msisdn = msisdn;
        }

    }

    {
        response = new Balance();
    }
}