package com.grameenphone.wipro.fmfs.mfs_communicator.model.sms;

public class SmsAccesInfo {
    private String accesskey;
    private String endUserId;
    private String accesschannel;
    private String referenceCode;
    private String servicekey;
    private String serviceIdentifier;
    private String  user;
	private String password;
	
	

    public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAccesskey() {
        return accesskey;
    }

    public void setAccesskey(String accesskey) {
        this.accesskey = accesskey;
    }

    public String getEndUserId() {
        return endUserId;
    }

    public void setEndUserId(String endUserId) {
        this.endUserId = endUserId;
    }

    public String getAccesschannel() {
        return accesschannel;
    }

    public void setAccesschannel(String accesschannel) {
        this.accesschannel = accesschannel;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getServicekey() {
        return servicekey;
    }

    public void setServicekey(String servicekey) {
        this.servicekey = servicekey;
    }

    public String getServiceIdentifier() {
        return serviceIdentifier;
    }

    public void setServiceIdentifier(String serviceIdentifier) {
        this.serviceIdentifier = serviceIdentifier;
    }
}
