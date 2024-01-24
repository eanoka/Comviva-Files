package com.grameenphone.wipro.fmfs.cbp.model.api.comm_module;

import com.grameenphone.wipro.fmfs.cbp.model.api.comm_module.BalanceResponse.Balance;

import java.util.Date;

public class BalanceResponse extends BaseResponse<Balance> {
    public static class Balance {
        public double balance;
        public String msisdn;
        public Date last_refill_on;
        public int last_refill_amount;
    }

    {
        response = new Balance();
    }
}
