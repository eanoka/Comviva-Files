package com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

public class NescoWasionPrepaidResponse {
    public String resultcode;
    public String resultdesc;
    public Data data;

    public static class Data {
        public String meterNo;
        public String transactionId;
        public String customerNo;
        public String customerName;
        public Map<String, Object> fee;

        public String orderNo;
        @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
        public Date orderDate;
        public String token;
    }
}
