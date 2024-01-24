package com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Entity
public class RebBillDetail {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;
    public String smsAccountNumber;
    @Column(insertable = false, updatable = false)
    public Date lastFetchingDate;
    public String brebResponse;
}