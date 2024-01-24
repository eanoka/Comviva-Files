package com.grameenphone.wipro.task_executor.enums;

import com.grameenphone.wipro.task_executor.util.orm.DefaultEnumValue;
import com.grameenphone.wipro.task_executor.util.orm.SafeEnumType;

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