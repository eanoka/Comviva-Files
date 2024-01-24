package com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "request_histories")
public class RequestHistories {
    @Id
    @Column(name = "txn_id", nullable = false)
    private String txnID;

    @Column(name = "consumer_id", nullable = false)
    private String consumer_id;

    @Column(name = "bill_number", nullable = false)
    private String billNumber;

    @Column(name = "company", nullable = false)
    private String company;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "request_initiated_time")
    private Timestamp requestInitiatedTime;

    @Column(name = "responded_time")
    private Timestamp respondedTime;

    @Column(name = "status_code")
    private String statusCode;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "message")
    private String message;

    @Column(name = "orderId")
    private String orderId;

    @Column(name = "orderStatus")
    private String orderStatus;

    @Column(name = "service_request_id")
    private String serviceRequestId;

    @Column(name = "mobiquity_txn_id")
    private String mobiquityTxnID;

    @Column(name = "mobiquity_txn_status")
    private String mobiquityTxnStatus;

    @Column(name = "mobiquity_message")
    private String mobiquityMessage;

    @Column(name = "third_party_txn_id")
    private String thirdPartyTxnID;

    @Column(name = "third_party_txn_status")
    private String thirdPartyTxnStatus;

    @Column(name = "third_party_message")
    private String thirdPartyMessage;

}
