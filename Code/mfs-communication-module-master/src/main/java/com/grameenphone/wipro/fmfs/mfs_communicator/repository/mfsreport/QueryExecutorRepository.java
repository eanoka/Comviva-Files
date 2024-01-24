package com.grameenphone.wipro.fmfs.mfs_communicator.repository.mfsreport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManagerFactory;

@Repository("mfsReportQueryExecutorRepository")
public class QueryExecutorRepository extends com.grameenphone.wipro.fmfs.mfs_communicator.repository.QueryExecutorRepository {
    @Autowired
    @Qualifier("mfsReportEntityManagerFactory")
    private void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }
}