package com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;

@Repository
public interface BillPayServiceStatusRepository extends JpaRepository<BillPayServiceStatus, Integer> {
    @Query("SELECT e FROM BillPayServiceStatus e WHERE e.companyCode = ?1 and e.accountNo = ?2 and e.amount = ?3 and e.creationDate >= ?4 and e.status = 'Dispute'")
    List<BillPayServiceStatus> findCurrentDateDisputeTxnByAccountAndAmount(String companyCode, String conusmerId, double amount, Date date);

    @Query("SELECT d FROM BillPayServiceStatus d WHERE d.companyCode = ?1 AND d.accountNo = ?2 order by d.creationDate desc")
    BillPayServiceStatus getLastBillPayServiceStatusByCompanyCodeAndAccountNo(String companyCode, String accountNo);

    @Query("SELECT s FROM BillPayServiceStatus s WHERE s.companyCode = ?1 AND s.creationDate >= ?2 ORDER BY s.attr1 DESC")
    List<BillPayServiceStatus> getScrollNumber(String companyCode, Date currentDate);

    @Query("SELECT j from BillPayServiceStatus j WHERE j.orderID =?1")
    BillPayServiceStatus findByOrderID(String orderID);
}
