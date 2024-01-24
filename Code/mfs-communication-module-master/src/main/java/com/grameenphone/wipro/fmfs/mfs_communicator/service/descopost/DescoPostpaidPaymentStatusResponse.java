package com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class DescoPostpaidPaymentStatusResponse extends DescoPostpaidBaseResponse{

	public static class Data {
		public String address;
		public int billingMonth;
		public String departmentCode;
		public int stampQty;
		public double paymentAmount;
		public double paymentVatAmount;
		public String collectionDate;
		public String transactionId;
		public String paymentChannel;
		public int billingYear;
		public String billNumber;
		public String paymentDate;
		public String paymentStatus;
		public String consumerName;
	}

	public List<Data> data;
}
