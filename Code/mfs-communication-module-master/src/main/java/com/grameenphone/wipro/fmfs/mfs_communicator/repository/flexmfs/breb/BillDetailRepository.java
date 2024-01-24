package com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.breb;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.RebBillDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface BillDetailRepository extends JpaRepository<RebBillDetail, Integer> {
    @Query("SELECT e FROM RebBillDetail e WHERE e.lastFetchingDate >= ?2 and e.smsAccountNumber = ?1")
    RebBillDetail getCachedBillDetailByAccountNumber(String smsAccountNumber, Date date);
    RebBillDetail findBySmsAccountNumber(String smsAccountNumber);
}