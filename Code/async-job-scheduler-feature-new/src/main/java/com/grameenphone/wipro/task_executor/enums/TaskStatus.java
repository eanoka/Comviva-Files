package com.grameenphone.wipro.task_executor.enums;

import com.grameenphone.wipro.task_executor.util.orm.DefaultEnumValue;
import com.grameenphone.wipro.task_executor.util.orm.SafeEnumType;

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