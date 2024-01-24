package com.grameenphone.wipro.task_executor.model.orm.fmfs;

import com.grameenphone.wipro.task_executor.enums.BillPaymentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JavaType;

@Entity
public class BillPayServiceStatus {
    @Id
    @GeneratedValue(
            strategy = GenerationType.AUTO,
            generator = "native"
    )
    @GenericGenerator(
            name = "native"
    )
    public long id;
    public String accountNo;
    public String billNo;
    public String mfsTxnid;
    @Enumerated(EnumType.STRING)
    @JavaType(BillPaymentStatus.BillPaymentStatusSafeEnum.class)
    public BillPaymentStatus status;
    public String companyCode;

    @Column(name = "order_id")
    public String orderID;
}