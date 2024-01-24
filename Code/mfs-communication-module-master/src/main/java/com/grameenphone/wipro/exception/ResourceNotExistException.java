package com.grameenphone.wipro.exception;

public class ResourceNotExistException extends HttpErrorResponseException {
    public ResourceNotExistException(String message) {
        super(404, message);
    }
}