package com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.PrepaidBillToken;

@Repository
public interface PrepaidBillTokenRepository extends JpaRepository<PrepaidBillToken, Long> {
    PrepaidBillToken findByBillPayTableId(long billpayId);
}