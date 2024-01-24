package com.grameenphone.wipro.fmfs.mfs_communicator.model.reb;

public class RebAuthenticationApiResponse {
	
	public RebAuthenticationApiResponseData DATA;
	public JsonResponseProperty RESPONSE;
	
	public RebAuthenticationApiResponseData getDATA() {
		return DATA;
	}
	public void setDATA(RebAuthenticationApiResponseData dATA) {
		DATA = dATA;
	}
	public JsonResponseProperty getRESPONSE() {
		return RESPONSE;
	}
	public void setRESPONSE(JsonResponseProperty rESPONSE) {
		RESPONSE = rESPONSE;
	}
	
	
	
	
}
