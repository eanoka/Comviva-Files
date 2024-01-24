package com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.mfsreport;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.ServiceChargePaidAmount;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.BalanceResponse.Balance;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.single_result.DoubleResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.single_result.IntegerResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.summary.BillData;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import java.util.Date;

@SqlResultSetMappings({
        @SqlResultSetMapping(name = "com.grameenphone.wipro.fmfs.mfs_communicator.model.summary.BillData", classes = @ConstructorResult(targetClass = BillData.class, columns = {
                @ColumnResult(name = "companyCode", type = String.class),
                @ColumnResult(name = "accountNumber", type = String.class),
                @ColumnResult(name = "billNumber", type = String.class),
                @ColumnResult(name = "month", type = String.class),
                @ColumnResult(name = "dueDate", type = Date.class),
                @ColumnResult(name = "amount", type = int.class),
                @ColumnResult(name = "status", type = String.class),
                @ColumnResult(name = "vat", type = double.class)
        })),
        @SqlResultSetMapping(name = "com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.BalanceResponse$Balance", classes = @ConstructorResult(targetClass = Balance.class, columns = {
                @ColumnResult(name = "balance", type = Double.class),
                @ColumnResult(name = "time", type = Date.class),
                @ColumnResult(name = "lastCrAmount", type = Integer.class),
                @ColumnResult(name = "msisdn", type = String.class)
        })),
        @SqlResultSetMapping(name = "com.grameenphone.wipro.fmfs.mfs_communicator.model.single_result.DoubleResult", classes = @ConstructorResult(targetClass = DoubleResult.class, columns = {
                @ColumnResult(name = "value", type = Double.class)
        })),
        @SqlResultSetMapping(name = "com.grameenphone.wipro.fmfs.mfs_communicator.model.single_result.IntegerResult", classes = @ConstructorResult(targetClass = IntegerResult.class, columns = {
                @ColumnResult(name = "value", type = Integer.class)
        })),
        @SqlResultSetMapping(name = "com.grameenphone.wipro.fmfs.mfs_communicator.model.ServiceChargePaidAmount", classes = @ConstructorResult(targetClass = ServiceChargePaidAmount.class, columns = {
                @ColumnResult(name = "paidAmount", type = Double.class),
                @ColumnResult(name = "serviceCharge", type = Double.class),
        }))
})
@Entity
/**
 * This class is to bind resultset mapping
 */
public class NotForUsingAsEntity {
    @Id
    int id;
}