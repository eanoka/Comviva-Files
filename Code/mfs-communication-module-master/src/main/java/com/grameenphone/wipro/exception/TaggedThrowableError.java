package com.grameenphone.wipro.exception;

public class TaggedThrowableError extends TaggedCheckedException { //Extending error to prevent rollback in transactional block
    public TaggedThrowableError(Throwable h) {
        super(h);
    }
}