package com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost;

public class DescoPostpaidTokenResponse extends DescoPostpaidBaseResponse {

	private String username;
	private String accessToken;
	private String tokenType;
	private long expirationTimeInMilli;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public long getExpirationTimeInMilli() {
		return expirationTimeInMilli;
	}

	public void setExpirationTimeInMilli(long expirationTimeInMilli) {
		this.expirationTimeInMilli = expirationTimeInMilli;
	}
}
