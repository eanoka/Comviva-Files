package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class WZPDCLPostpaidService extends SummaryDbBasedCompanyService {
    @Bean({"WZPD_Bill_Fetcher", "WZPD_Bill_Payer"})
    public WZPDCLPostpaidService alias() {
        return this;
    }
    {
        companyCode = "WZPD";
    }
}