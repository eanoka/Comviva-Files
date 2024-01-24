package com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface DisputeTransactionRepository extends JpaRepository<DisputeTransaction, Integer> {
    @Query("SELECT e FROM DisputeTransaction e WHERE e.status = 'PENDING' AND e.creationDate >= ?1")
    List<DisputeTransaction> getDisputeTransaction(Date date);
    List<DisputeTransaction> getDisputeTransactionByStatus(String status);

    @Query("SELECT d FROM DisputeTransaction d WHERE d.status = 'PENDING' AND d.creationDate >= ?2 and d.billPayServiceStatus.companyCode = ?1")
    List<DisputeTransaction> getDisputeTransactionByCompanyCode(String companyCode, Date sinceTime);

    @Query("SELECT d FROM DisputeTransaction d WHERE d.status = 'PENDING' AND d.billPayServiceStatus.companyCode = ?1 AND d.billPayServiceStatus.accountNo = ?2 order by d.creationDate desc")
    DisputeTransaction getLastPendingDisputeTransaction(String companyCode, String consumerId);
}