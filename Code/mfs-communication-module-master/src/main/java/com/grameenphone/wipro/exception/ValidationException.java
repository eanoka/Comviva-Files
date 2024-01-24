package com.grameenphone.wipro.exception;

public class ValidationException extends HttpErrorResponseException {
    public ValidationException(String message) {
        super(422, message);
    }

    public ValidationException(int code, String message) {
        super(code, message);
    }
}