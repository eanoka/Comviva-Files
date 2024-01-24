package com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost;

public class DescoPostpaidBaseResponse {
	private String status;
	private String statusCode;
	private String message;	
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}	
}
