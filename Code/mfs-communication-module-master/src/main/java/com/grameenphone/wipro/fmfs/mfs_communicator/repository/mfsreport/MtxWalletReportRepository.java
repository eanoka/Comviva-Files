package com.grameenphone.wipro.fmfs.mfs_communicator.repository.mfsreport;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.mfsreport.MtxWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MtxWalletReportRepository extends JpaRepository<MtxWallet, String> {
    MtxWallet findByMsisdnAndStatus(String msisdn, String status);
}