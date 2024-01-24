package com.grameenphone.wipro.fmfs.mfs_communicator.model.reb;

public class JsonResponseProperty {
    public String RESPONSE_MSG;
    public int RESPONSE_CODE;

    public String getRESPONSE_MSG() {
        return RESPONSE_MSG;
    }

    public void setRESPONSE_MSG(String rESPONSE_MSG) {
        RESPONSE_MSG = rESPONSE_MSG;
    }

    public int getRESPONSE_CODE() {
        return RESPONSE_CODE;
    }

    public void setRESPONSE_CODE(int rESPONSE_CODE) {
        RESPONSE_CODE = rESPONSE_CODE;
    }
}