package com.grameenphone.wipro.fmfs.mfs_communicator.model.reb;

public class NewTokenAPIResponse {
    public RebTokenResponseData DATA; //There will be value only if response contains token (new token API)
    public JsonResponseProperty RESPONSE;

    public RebTokenResponseData getDATA() {
        return DATA;
    }

    public void setDATA(RebTokenResponseData dATA) {
        DATA = dATA;
    }

    public JsonResponseProperty getRESPONSE() {
        return RESPONSE;
    }

    public void setRESPONSE(JsonResponseProperty rESPONSE) {
        RESPONSE = rESPONSE;
    }
}