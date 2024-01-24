package com.grameenphone.wipro.fmfs.mfs_communicator.model.dpdcpostpaid;

import com.fasterxml.jackson.annotation.JsonAlias;

public class DpdcPostpaidPayBillRespose {
    @JsonAlias("PAYMENT STATUS")
	public String paymentStatus;
}
