package com.grameenphone.wipro.exception;

public class ServiceProcessingError extends Error {
    public int status;

    public ServiceProcessingError(String message) {
        super(message);
        this.status = 520;
    }

    public ServiceProcessingError(String message, int status) {
        super(message);
        this.status = status;
    } 

    public ServiceProcessingError(String message, int status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
