package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import com.grameenphone.wipro.enums.DisputeTransactionStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.DisputeTransactionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DisputeService {
    @Value("${dispute_resolver_node_id}")
    String disputeNodeId;

    @Autowired
    DisputeTransactionRepository disputeTransactionRepository;

    protected static final Logger logger = LoggerFactory.getLogger(PdbLikePaymentService.class);

    public void insertDisputeRecord(BillPayServiceStatus billPayServiceStatus, String msisdn, String custMsisdn) {
        insertDisputeRecord(billPayServiceStatus, msisdn, custMsisdn, null);
    }

    public void insertDisputeRecord(BillPayServiceStatus billPayServiceStatus, String msisdn, String custMsisdn, String attr4) {
        DisputeTransaction disputeTransaction = new DisputeTransaction();
        disputeTransaction.setAmount(billPayServiceStatus.getAmount());
        disputeTransaction.setBillNo(billPayServiceStatus.getBillNo());
        disputeTransaction.setCustomerMsisdn(custMsisdn);
        disputeTransaction.setInitiator(msisdn);
        disputeTransaction.setMeterNo(billPayServiceStatus.getAccountNo());
        disputeTransaction.setBillPayServiceStatus(billPayServiceStatus);
        disputeTransaction.setStatus(DisputeTransactionStatus.PENDING.toString());
        disputeTransaction.setAttr_1(disputeNodeId);
        disputeTransaction.setAttr_4(attr4);

        disputeTransactionRepository.save(disputeTransaction);
        logger.debug("Saved dispute transaction id : " + disputeTransaction.getId());
    }
}