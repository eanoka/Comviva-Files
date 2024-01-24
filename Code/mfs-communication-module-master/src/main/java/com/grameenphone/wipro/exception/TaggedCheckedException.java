package com.grameenphone.wipro.exception;

public class TaggedCheckedException extends Error { //Extending error to prevent rollback in transactional block
    public TaggedCheckedException(Throwable h) {
        super(h);
    }
}