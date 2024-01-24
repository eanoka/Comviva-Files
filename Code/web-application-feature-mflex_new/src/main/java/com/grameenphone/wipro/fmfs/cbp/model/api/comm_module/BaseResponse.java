package com.grameenphone.wipro.fmfs.cbp.model.api.comm_module;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public abstract class BaseResponse<T> {
    public long timestamp = System.currentTimeMillis();
    public int status = 200;
    public String message = "success";
    @JsonInclude(Include.NON_NULL)
    public T response;
}