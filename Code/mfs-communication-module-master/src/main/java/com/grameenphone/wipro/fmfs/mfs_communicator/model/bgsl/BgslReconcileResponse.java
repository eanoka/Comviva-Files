package com.grameenphone.wipro.fmfs.mfs_communicator.model.bgsl;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BgslReconcileResponse {
	@JsonProperty("timestamp")
	public String timestamp;
	@JsonProperty("status")
	public String status;
	@JsonProperty("statusCode")
	public Integer statusCode;
	@JsonProperty("message")
	public String message;
	public List<Content> content;

	public static class Content {

		@JsonProperty("Transaction Amount")
		public String transaction_amount;
		@JsonProperty("Transaction Number")
		public String transaction_number;

	}
}
