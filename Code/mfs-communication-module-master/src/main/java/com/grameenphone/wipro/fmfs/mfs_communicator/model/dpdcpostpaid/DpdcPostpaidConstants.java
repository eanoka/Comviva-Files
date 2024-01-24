package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdcpostpaid;

import java.util.HashMap;
import java.util.Map;

public class DpdcPostpaidConstants {
	public static final Map<String, String> responseMessages = new HashMap<>() {
		{
			put("N","Bill not Paid");
			put("K","No Data Found or Customer Number Invalid or Transaction ID invalid");
			put("D","Duplicate Transaction ID");
			put("I","Bank Code and User Invalid");
			put("S","Bill Payment Successful");
			put("E","Bill Already Exists");
			put("B","Bill Number/Account Number Invalid");
			put("L","Location code Invalid");
			put("T","Bill Amount Mismatch");
			put("V","Vat Amount Mismatch");
			put("U","Bill Unsuccessful");
			put("C","Pay Chanel Invalid");
			put("M","Total Amount Mismatch");
			put("F","CD Mismatch");
		}
	};
}
