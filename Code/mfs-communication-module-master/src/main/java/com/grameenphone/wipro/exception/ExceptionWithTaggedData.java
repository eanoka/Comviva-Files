package com.grameenphone.wipro.exception;

public class ExceptionWithTaggedData extends Exception {
    public Object taggedData;
    
    public ExceptionWithTaggedData(Object data) {
        this(data, "");
    }
    
    public ExceptionWithTaggedData(Object data, String message) {
        this(data, null, message);
    }
    
    public ExceptionWithTaggedData(Object data, Throwable m) {
        this(data, m, "");
    }
    
    public ExceptionWithTaggedData(Object data, Throwable m, String message) {
        super(message, m);
        taggedData = data;
    }
}