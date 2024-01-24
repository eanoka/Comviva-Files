package com.grameenphone.wipro.fmfs.cbp.model.view.generic;

public class BaseApiResponse {
    public String message;
    public int code = 200;

    public BaseApiResponse() {}

    public BaseApiResponse(String message) {
        this.message = message;
    }
}