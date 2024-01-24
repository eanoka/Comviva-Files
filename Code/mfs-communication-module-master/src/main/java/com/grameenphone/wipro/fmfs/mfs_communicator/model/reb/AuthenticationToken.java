package com.grameenphone.wipro.fmfs.mfs_communicator.model.reb;

public class AuthenticationToken {
	

    public String REFRESH_TOKEN;
    public String ACCESS_TOKEN;
     
	public String getREFRESH_TOKEN() {
		return REFRESH_TOKEN;
	}
	public void setREFRESH_TOKEN(String rEFRESH_TOKEN) {
		REFRESH_TOKEN = rEFRESH_TOKEN;
	}
	public String getACCESS_TOKEN() {
		return ACCESS_TOKEN;
	}
	public void setACCESS_TOKEN(String aCCESS_TOKEN) {
		ACCESS_TOKEN = aCCESS_TOKEN;
	}
	
}
