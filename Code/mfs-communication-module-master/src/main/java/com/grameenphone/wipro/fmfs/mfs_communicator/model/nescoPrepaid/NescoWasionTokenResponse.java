package com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid;

public class NescoWasionTokenResponse {

	public String resultcode;
	public String resultdesc;
	private long  expirationTimeInMilli;
	public Data data;

	public class Data {
		public String access_token;
		public String balance;
		public boolean firstLogin;
	}

	public long getExpirationTimeInMilli() {
		return expirationTimeInMilli;
	}

	public void setExpirationTimeInMilli(long expirationTimeInMilli) {
		this.expirationTimeInMilli = expirationTimeInMilli;
	}
}
