package com.grameenphone.wipro.fmfs.mfs_communicator.model.bgsl;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BgslGetBillResponse {
    @JsonProperty("timestamp")
    public String timestamp;
    @JsonProperty("status")
    public String status;
    @JsonProperty("statusCode")
    public Integer statusCode;
    @JsonProperty("message")
    public String message;
    public List<Content> content;

    public static class Content {
        @JsonProperty("NAME")
        public String name;
        @JsonProperty("Category")
        public String category;
        @JsonProperty("Zone")
        public String zone;
        @JsonProperty("Current Total(Surch Incl.)")
        public Integer currentTotal;
        @JsonProperty("Surcharge Amount")
        public Integer surchargeAmount;
        @JsonProperty("Last Payment Date")
        public String lastPaymentDate;
        @JsonProperty("Billing Months")
        public String billingMonths;
        @JsonProperty("Is Govt.")
        public boolean isGovt;
    }
}
