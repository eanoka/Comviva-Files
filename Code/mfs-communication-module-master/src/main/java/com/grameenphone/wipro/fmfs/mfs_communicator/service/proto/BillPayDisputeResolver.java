package com.grameenphone.wipro.fmfs.mfs_communicator.service.proto;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;

/**
 * Implementor of this class must have a constructor with DisputeTransaction type argument
 */
public interface BillPayDisputeResolver {
    void resolveDispute(DisputeTransaction disputeTransaction);
}