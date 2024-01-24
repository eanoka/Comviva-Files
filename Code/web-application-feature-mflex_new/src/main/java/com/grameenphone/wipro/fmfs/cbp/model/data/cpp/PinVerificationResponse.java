package com.grameenphone.wipro.fmfs.cbp.model.data.cpp;

public class PinVerificationResponse {
	public static class Result {
		public boolean valid;
	}

	public long timestamp;
	public int status;
	public String message;
	public Result response;
}