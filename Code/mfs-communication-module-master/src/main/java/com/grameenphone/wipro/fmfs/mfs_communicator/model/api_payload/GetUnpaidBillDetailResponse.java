package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class GetUnpaidBillDetailResponse extends BaseResponse<GetUnpaidBillDetailResponse.BillDetail> {

	{
		response = new BillDetail();
	}

	public static class Data {
		@JsonInclude(Include.NON_NULL)
		public String billNo;
		@JsonInclude(Include.NON_NULL)
	    public String accountNo;
		@JsonInclude(Include.NON_NULL)
	    public String meterNo;
		@JsonInclude(Include.NON_NULL)
	    public String year;
		@JsonInclude(Include.NON_NULL)
	    public String month;
		@JsonInclude(Include.NON_NULL)
	    public double totalAmount;
		@JsonInclude(Include.NON_NULL)
	    public double totalVat;
		@JsonInclude(Include.NON_NULL)
	    public String issueDate;
		@JsonInclude(Include.NON_NULL)
	    public String dueDate;
		@JsonInclude(Include.NON_NULL)
	    public double totalAmountToBePaid;
		@JsonInclude(Include.NON_NULL)
		public Object detail;
	}

	public static class BillDetail {
		public String company;
		public String consumerId;
		public Data data;
	}
}