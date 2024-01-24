package com.grameenphone.wipro.fmfs.mfs_communicator.model.bgsl;

public class BgslPayBillResponse {
    public String timestamp;
    public String status;
    public Integer statusCode;
    public String message;
    public Content content;

    public static class Content {
        public String txId;
    }
}
