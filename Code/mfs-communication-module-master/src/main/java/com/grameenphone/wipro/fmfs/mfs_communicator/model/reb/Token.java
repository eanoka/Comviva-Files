package com.grameenphone.wipro.fmfs.mfs_communicator.model.reb;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.time.Instant;

public class Token {
   
    public String REFRESH_TOKEN;
    public String ACCESS_TOKEN;
  //  @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss.SSSSSS XXX", timezone = "Asia/Dhaka")
  //  Instant TIMESTAMP;
    @JsonInclude(Include.NON_NULL)
    public Integer NO_OF_ATTEMPT;
    
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
	/*
	public Instant getTIMESTAMP() {
		return TIMESTAMP;
	}
	public void setTIMESTAMP(Instant tIMESTAMP) {
		TIMESTAMP = tIMESTAMP;
	}
	**/
	public Integer getNO_OF_ATTEMPT() {
		return NO_OF_ATTEMPT;
	}
	public void setNO_OF_ATTEMPT(Integer nO_OF_ATTEMPT) {
		NO_OF_ATTEMPT = nO_OF_ATTEMPT;
	}
    
    
    
}