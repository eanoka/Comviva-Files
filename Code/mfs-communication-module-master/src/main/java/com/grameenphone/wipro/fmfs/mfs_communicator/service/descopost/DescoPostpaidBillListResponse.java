package com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost;

import java.util.List;

public class DescoPostpaidBillListResponse extends DescoPostpaidBaseResponse{
	
	public List<DescoPostpaidBillListDataResponse> response;

	public List<DescoPostpaidBillListDataResponse> getResponse() {
		return response;
	}

	public void setResponse(List<DescoPostpaidBillListDataResponse> response) {
		this.response = response;
	}
}
