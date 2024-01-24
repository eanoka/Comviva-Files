package com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class RebAuthenticationApiDetail {

	@Id
	@Column(name = "id", nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public int id;

	@Column(length = 65555) 
	public String refresh_token;  
	
	@Column(length = 65555) 
	public String access_token;  
	 
}
