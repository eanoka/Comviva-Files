package com.grameenphone.wipro.fmfs.cbp.enums;

import com.grameenphone.wipro.extensions.spring.boot.orm.DefaultEnumValue;
import com.grameenphone.wipro.extensions.spring.boot.orm.SafeEnumType;

@DefaultEnumValue("Unknown")
public enum BillStatus {
    Unpaid,
    InProcess,
    Success,
    Fail,
    Dispute,
    Obsolete,
    Unknown; //To use as default in conversion by orm

    public static class BillStatusSafeEnum extends SafeEnumType<BillStatus> {
        public BillStatusSafeEnum() {
            super(BillStatus.class);
        }
    }
}