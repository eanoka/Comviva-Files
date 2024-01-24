package com.grameenphone.wipro.task_executor.model.orm.cbp;

import com.grameenphone.wipro.task_executor.enums.TaskStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JavaType;

import java.sql.Timestamp;

@Entity
public class PaymentTask {
    @Id
    @GeneratedValue(
            strategy = GenerationType.AUTO,
            generator = "native"
    )
    @GenericGenerator(
            name = "native",
            strategy = "native"
    )
    public long id;
    public Integer requestHopId;
    public String nodeId;
    public String pin;
    @Enumerated(EnumType.STRING)
    @JavaType(TaskStatus.TaskStatusSafeEnum.class)
    public TaskStatus status;
    public Integer totalProcessed;
    public Integer successCount;
    public Integer failedCount;
    public Integer disputeCount;
    public Timestamp startTime;
    public Timestamp endTime;
}