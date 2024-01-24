package com.grameenphone.wipro.fmfs.mfs_communicator.model.sslwireless;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SSLWirelessBillInfoResponse {

	public String status;
	public String message;
	@JsonProperty("status_code")
	public Integer statusCode;
	@JsonProperty("status_title")
	public String statusTitle;
	public String lid;
	@JsonProperty("transaction_id")
	public String transactionId;
	public Map<String, String> data;
}