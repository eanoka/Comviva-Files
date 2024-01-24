package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.util.Collection;
import java.util.Date;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author wipro.tribhuwan
 *
 */
@Entity
public class PaymentRequestHop {
	//region Auto
	@Id
	@GeneratedValue(
			strategy = GenerationType.AUTO,
			generator = "native"
	)
	@GenericGenerator(
			name = "native",
			strategy = "native"
	)
    private long id;

	@Column(updatable = false, insertable = false)
	private Date initiationTime;
	//endregion

	//region on execution
	private Date executionTime;
      
    private String comment;
    	
	@ManyToOne(fetch = FetchType.EAGER)
    private User executedBy;
	//endregion

	//region creation
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
    private PaymentRequest request;

	@OneToOne(fetch = FetchType.EAGER)
    private PaymentRequestHop previousHop;

	@ManyToOne
    private WorkflowHop workflowHop;

	@OneToMany
    private Collection<User> possibleExecutors;
	//endregion

	//region non persistent
	@OneToOne(mappedBy = "requestHop")
	private PaymentTask task;
	//endregion

	public PaymentRequest getPaymentRequest() {
		return request;
	}

	public void setPaymentRequest(PaymentRequest paymentRequest) {
		this.request = paymentRequest;
	}

	public WorkflowHop getWorkflowHop() {
		return workflowHop;
	}

	public void setWorkflowHop(WorkflowHop workflowHop) {
		this.workflowHop = workflowHop;
	}

	public Date getInitiationTime() {
		return initiationTime;
	}

	public void setInitiationTime(Date initiationTime) {
		this.initiationTime = initiationTime;
	}

	public User getExecutedBy() {
		return executedBy;
	}

	public void setExecutedBy(User executedBy) {
		this.executedBy = executedBy;
	}

	public Date getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(Date executionTime) {
		this.executionTime = executionTime;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public PaymentTask getPaymentTask() {
		return task;
	}

	public void setPaymentTask(PaymentTask paymentTask) {
		this.task = paymentTask;
	}

	public long getId() {
		return id;
	}

	public Collection<User> getPossibleExecutors() {
		return possibleExecutors;
	}

	public void setPossibleExecutors(Collection<User> possibleExecutors) {
		this.possibleExecutors = possibleExecutors;
	}

	public PaymentRequestHop getPreviousHop() {
		return previousHop;
	}

	public void setPreviousHop(PaymentRequestHop previousHop) {
		this.previousHop = previousHop;
	}
}