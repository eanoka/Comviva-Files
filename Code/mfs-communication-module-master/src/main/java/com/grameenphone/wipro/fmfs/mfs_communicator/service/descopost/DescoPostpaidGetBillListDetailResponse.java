package com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DescoPostpaidGetBillListDetailResponse extends DescoPostpaidBaseResponse{

	public static class Data {
		public String accountNo;
		public String billNo;
		public String billMonth;
		public String billYear;
		public String totalKwh;
		public double amount;
		public double lpc;
		public double vat;
		public String issueDate;
		public String dueDate;
		public String paymentStatus;
	}

	public List<Data> data;
}