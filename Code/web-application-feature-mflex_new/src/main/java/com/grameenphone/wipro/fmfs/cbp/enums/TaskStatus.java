package com.grameenphone.wipro.fmfs.cbp.enums;

import com.grameenphone.wipro.extensions.spring.boot.orm.DefaultEnumValue;
import com.grameenphone.wipro.extensions.spring.boot.orm.SafeEnumType;

@DefaultEnumValue("Unknown")
public enum TaskStatus {
    Pending,
    Processing,
    Completed,
    Unknown; //To use as default in conversion by orm

    public static class TaskStatusSafeEnum extends SafeEnumType<TaskStatus> {
        public TaskStatusSafeEnum() {
            super(TaskStatus.class);
        }
    }
}