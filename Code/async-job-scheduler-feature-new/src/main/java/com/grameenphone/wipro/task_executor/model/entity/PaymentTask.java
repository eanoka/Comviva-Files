package com.grameenphone.wipro.task_executor.model.entity;

import com.grameenphone.wipro.task_executor.enums.TaskStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;

import java.sql.Timestamp;

@Entity
public class PaymentTask {
    @Id
    public Long id;
    public Long requestHopId;
    public String nodeId;
    public String pin;
    @Enumerated(EnumType.STRING)
    @JavaType(TaskStatus.TaskStatusSafeEnum.class)
    public TaskStatus status = TaskStatus.Pending;
    public Integer totalProcessed;
    public Integer successCount;
    public Integer failedCount;
    public Integer disputeCount;
    public Timestamp startTime;
    public Timestamp endTime;

    @Transient
    public Timestamp creationTime;

    @Transient
    public Long requestId;
}