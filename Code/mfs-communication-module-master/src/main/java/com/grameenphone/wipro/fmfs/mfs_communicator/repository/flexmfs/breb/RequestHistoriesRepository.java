package com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.breb;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.RequestHistories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestHistoriesRepository extends JpaRepository<RequestHistories, String> {
    @Query("SELECT j from RequestHistories j WHERE j.orderId =?1")
    public RequestHistories findByOrderId(String orderId);
}
