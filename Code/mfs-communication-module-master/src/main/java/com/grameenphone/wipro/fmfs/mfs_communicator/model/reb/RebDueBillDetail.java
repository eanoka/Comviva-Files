package com.grameenphone.wipro.fmfs.mfs_communicator.model.reb;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

public class RebDueBillDetail {

	
	 public String BILL_NO;
     public String SMS_AC_NO;
     public String BILL_MONTH;
     public String BILL_YEAR;
     @JsonFormat(pattern = "dd/MM/yyyy", timezone = "Asia/Dhaka")
     public Date ISSUE_DATE;
     @JsonFormat(pattern = "dd/MM/yyyy", timezone = "Asia/Dhaka")
     public Date DUE_DATE;
     public String PBS_NAME_E;

     
}
