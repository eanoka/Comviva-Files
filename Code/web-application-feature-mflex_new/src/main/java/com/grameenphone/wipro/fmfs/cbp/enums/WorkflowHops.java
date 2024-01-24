package com.grameenphone.wipro.fmfs.cbp.enums;

import com.grameenphone.wipro.extensions.spring.boot.orm.SafeEnumType;

public enum WorkflowHops {
    WFA,
    WFA_DIS,
    WPI,
    SFE,
    CPL,
    REJ;

    public static class WorkflowHopsSafeEnum extends SafeEnumType<WorkflowHops> {
        public WorkflowHopsSafeEnum() {
            super(WorkflowHops.class);
        }
    }
}