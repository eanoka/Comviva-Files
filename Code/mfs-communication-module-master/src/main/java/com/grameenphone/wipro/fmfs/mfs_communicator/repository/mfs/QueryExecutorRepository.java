package com.grameenphone.wipro.fmfs.mfs_communicator.repository.mfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManagerFactory;

@Repository("mfsQueryExecutorRepository")
public class QueryExecutorRepository extends com.grameenphone.wipro.fmfs.mfs_communicator.repository.QueryExecutorRepository {
    @Autowired
    @Qualifier("mfsEntityManagerFactory")
    private void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }
}