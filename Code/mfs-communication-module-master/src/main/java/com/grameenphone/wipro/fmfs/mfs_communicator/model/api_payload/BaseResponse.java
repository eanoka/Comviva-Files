package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public abstract class BaseResponse<T> {
    public long timestamp = System.currentTimeMillis();
    public int status = 200;
    public String message = "success";
    @JsonInclude(Include.NON_NULL)
    public T response;
}