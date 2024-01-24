package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.DueBills;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DueBillsResponse extends BaseResponse<DueBills> {

    {
        response = new DueBills();
    }

    public static class Bill {
        public String billNo;
        @JsonInclude(Include.NON_NULL)
        public String billMonthYear;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Dhaka")
        @JsonInclude(Include.NON_NULL)
        public Date billIssueDate;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Dhaka")
        public Date billDueDate;
        public int amount;
        @JsonInclude(Include.NON_NULL)
        public Double vat;
        public double serviceCharge;
        @JsonInclude(Include.NON_NULL)
        public Object detail;
    }

    public static class DueBills {
        public String company;
        public String consumerId;
        public List<Bill> bills = new ArrayList<>();
    }
}