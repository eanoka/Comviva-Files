package com.grameenphone.wipro.exception;

/**
 * Handle call back exception
 */
public class CallbackException extends TaggedCheckedException {
	public CallbackException(String message, Throwable p) {
		super(message, p);
	}
}