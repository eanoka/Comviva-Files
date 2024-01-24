package com.grameenphone.wipro.fmfs.mfs_communicator.model;

public class HttpResponse {
    public String response;
    public String responseCode;
    public boolean isError = false;

    public HttpResponse(boolean isError, String response, String responseCode) {
        this.isError = isError;
        this.response = response;
        this.responseCode = responseCode;
    }

    public HttpResponse(boolean isError, String response) {
        this.isError = isError;
        this.response = response;
    }

    public HttpResponse(String response) {
        this.response = response;
    }

    public boolean isError() {
        return isError;
    }

    public void setError(boolean isError) {
        this.isError = isError;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public HttpResponse() {}
}
