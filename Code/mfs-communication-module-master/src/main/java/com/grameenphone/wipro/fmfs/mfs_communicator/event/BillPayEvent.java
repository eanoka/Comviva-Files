package com.grameenphone.wipro.fmfs.mfs_communicator.event;

import com.grameenphone.wipro.enums.EventType;

public class BillPayEvent {
    public EventType eventType;
    public long timestamp = System.currentTimeMillis();
    public String initiator;
    public String initiator_on_behalf;
    public String initiator_grade;
    public String channel;
    public Data data;

    public static class Data {
        public double amount;
        public String utility;
        public double service_charge;
        public String mfs_txn_id;
    }

    {
        eventType = EventType.BILLPAY;
    }
}