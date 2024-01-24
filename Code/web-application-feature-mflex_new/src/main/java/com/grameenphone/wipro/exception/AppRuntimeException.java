package com.grameenphone.wipro.exception;

public class AppRuntimeException extends RuntimeException {
	public int status = 500;

	public AppRuntimeException(String message) {
		super(message);
	}

	public AppRuntimeException(String message, int status) {
		super(message);
		this.status = status;
	}

	public AppRuntimeException(int status) {
		super("");
		this.status = status;
	}
}