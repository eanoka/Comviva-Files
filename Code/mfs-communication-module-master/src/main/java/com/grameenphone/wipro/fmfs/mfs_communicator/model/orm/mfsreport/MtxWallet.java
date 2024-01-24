package com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.mfsreport;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class MtxWallet {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public String walletNumber;
    public String msisdn;
    public String userGrade;
    public String status;
}
