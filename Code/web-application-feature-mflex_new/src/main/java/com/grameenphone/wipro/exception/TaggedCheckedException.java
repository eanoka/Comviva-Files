package com.grameenphone.wipro.exception;

public class TaggedCheckedException extends RuntimeException {
    public TaggedCheckedException(Throwable h) {
        super(h);
    }

    public TaggedCheckedException(String message, Throwable h) {
        super(message, h);
    }
}