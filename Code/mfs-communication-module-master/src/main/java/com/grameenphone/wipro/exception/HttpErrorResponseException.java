package com.grameenphone.wipro.exception;

import java.io.IOException;

public class HttpErrorResponseException extends IOException {
	private int status;
	private String reason;
	private String body;

	public HttpErrorResponseException(int status, String reason, String content) {
		this.status = status;
		this.reason = reason;
		this.body = content;
	}

	public HttpErrorResponseException(int status, String reason) {
		this(status, reason, null);
	}

	public int getStatus() {
		return status;
	}

	public String getReason() {
		return reason;
	}

	public String getContent() {
		return body;
	}

	@Override
	public String getMessage() {
		return getReason();
	}
}