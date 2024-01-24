package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

public abstract class BillerRequest extends BaseRequest {
	private String company;
	private String mfsCompany;
	private String consumerId;

	public String getCompany() {
		return company;
	}

	public String getMFSCompany() {
		return mfsCompany;
	}

	public void setCompany(String company) {
		this.company = mfsCompany = company;
		if (company.contains(":")) {
			mfsCompany = company.substring(0, company.indexOf(':'));
		}
	}

	public String getConsumerId() {
		return consumerId;
	}

	public void setConsumerId(String consumerId) {
		this.consumerId = consumerId;
	}
}