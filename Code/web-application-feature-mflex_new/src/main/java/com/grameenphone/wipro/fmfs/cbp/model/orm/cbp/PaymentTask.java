package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.sql.Timestamp;
import java.util.Date;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JavaType;

import com.grameenphone.wipro.extensions.spring.boot.orm.SafeEnumType;
import com.grameenphone.wipro.fmfs.cbp.enums.TaskStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;



/**
 * @author wipro.tribhuwan
 *
 */
@Entity
public class PaymentTask {
	private static class EventTypeSafeEnum extends SafeEnumType<TaskStatus>{
		  public EventTypeSafeEnum() {
	            super(TaskStatus.class);
	        }
	}
	@Id
	@GeneratedValue(
			strategy = GenerationType.AUTO,
			generator = "native"
	)
	@GenericGenerator(
			name = "native",
			strategy = "native"
	)
	//region auto
    private long id;
	private Timestamp updateTime;
	private String nodeId;
	//endregion

	//region on create
	/*Pin should be saved temporary here*/
	private String pin;

	@OneToOne
	private PaymentRequestHop requestHop;

	@Enumerated(EnumType.STRING)
	@JavaType(EventTypeSafeEnum.class)
	private TaskStatus status = TaskStatus.Pending;
	//endregion

	//region to update
	private int totalProcessed;
	private int failedCount;
	private int successCount;
	private int disputeCount;
	private Date startTime;
	private Date endTime;
	//endregion

	public PaymentRequestHop getPaymentRequestHop() {
		return requestHop;
	}

	public void setPaymentRequestHop(PaymentRequestHop paymentRequestHop) {
		this.requestHop = paymentRequestHop;
	}

	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public int getTotalProcessed() {
		return totalProcessed;
	}

	public void setTotalProcessed(int totalProcessed) {
		this.totalProcessed = totalProcessed;
	}

	public int getFailedCount() {
		return failedCount;
	}

	public void setFailedCount(int failedCount) {
		this.failedCount = failedCount;
	}

	public int getSuccessCount() {
		return successCount;
	}

	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Timestamp getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Timestamp updateTime) {
		this.updateTime = updateTime;
	}

	public int getDisputeCount() {
		return disputeCount;
	}

	public void setDisputeCount(int disputeCount) {
		this.disputeCount = disputeCount;
	}
}