package com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.Map;

public class NescoPrepaidResponse {
	public String resultcode;
	public String resultdesc;
	public Data data;

	public static class Data {
		public int newCurrencyError;
		public String orderNo;
		public String meterNo;
		@JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
		public Date orderDate;
		public String transactionId;
		public String customerNo;
		public String customerName;
		public Map<String, Object> fee;
		public String token;
	}
}
