package com.grameenphone.wipro.fmfs.mfs_communicator.model.nesco;

public class BillPaymentResponse {
    private int status_code;
    private String status_title;
    private String status;
    private String message;
    private String lid;
    private NescoBillPaymentDetails data;

    public int getStatus_code() {
        return status_code;
    }

    public void setStatus_code(int status_code) {
        this.status_code = status_code;
    }

    public String getStatus_title() {
        return status_title;
    }

    public void setStatus_title(String status_title) {
        this.status_title = status_title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLid() {
        return lid;
    }

    public void setLid(String lid) {
        this.lid = lid;
    }

    public NescoBillPaymentDetails getData() {
        return data;
    }

    public void setData(NescoBillPaymentDetails data) {
        this.data = data;
    }

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
    
}