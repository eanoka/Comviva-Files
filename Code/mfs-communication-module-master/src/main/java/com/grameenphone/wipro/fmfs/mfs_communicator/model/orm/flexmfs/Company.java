package com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity(name = "api_bill_company_detail")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Company {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	public int id;
    @ManyToOne
    @Fetch(FetchMode.JOIN)
	public Category category;
	public String companyCode;
	public String companyName;
	public String status;
    public boolean hasSurcharge;
    public boolean supportDecimalAmount;
}