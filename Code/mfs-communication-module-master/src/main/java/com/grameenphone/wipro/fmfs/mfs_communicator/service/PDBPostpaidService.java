package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class PDBPostpaidService extends SummaryDbBasedCompanyService {
    @Bean({"BPDB_Bill_Fetcher", "BPDB_Bill_Payer"})
    public PDBPostpaidService alias() {
        return this;
    }

    {
        companyCode = "BPDB";
    }
}