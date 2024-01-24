package com.grameenphone.wipro.fmfs.cbp.model.view.report;

import java.util.Date;
import java.util.List;

public class RequestModal {
	public long id;
    public String requestor;
    public Date date;
    public List<CompanyInfo> companies;
    
    public RequestModal() {
    	
    }
    
	public RequestModal(long id, String requestor, Date date, List<CompanyInfo> companies) {
		this.id = id;
		this.requestor = requestor;
		this.date = date;
		this.companies = companies;
	}
	
	public RequestModal(long id, String requestor, Date date) {
		this.id = id;
		this.requestor = requestor;
		this.date = date;
	}
}
