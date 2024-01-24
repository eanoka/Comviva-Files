package com.grameenphone.wipro.exception;

import com.grameenphone.wipro.utility.common.HttpClient;

import java.io.IOException;

/**
 * @author wipro.zobair
 * @updated 11-11-21
 */
public class HttpErrorResponseException extends IOException {
	private int status;
	private String response;
	private String reason;

	public HttpErrorResponseException(int status, String response, String reason) {
		this.status = status;
		this.response = response;
		this.reason = reason;
	}

	public HttpErrorResponseException(HttpClient.HttpResponseSnapshot snapshot) {
		this.status = snapshot.status;
		this.response = snapshot.body;
		this.reason = snapshot.reason;
	}

	@Override
	public String getMessage() {
		return "Error (" + status + "): " + reason;
	}

	public int getStatus() {
		return status;
	}

	public String getResponse() {
		return response;
	}

	public String getReason() {
		return reason;
	}
}