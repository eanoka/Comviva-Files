package com.grameenphone.wipro.fmfs.mfs_communicator.model.sslwireless;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SSLWirelessApiResponse {

	public String status;
	public String message;
	@JsonProperty("status_code")
	public String statusCode;
	@JsonProperty("status_title")
	public String statusTitle;
	public String lid;
	@JsonProperty("transaction_id")
	public String transactionId;
	
	public Map<String, Object> data;
}
