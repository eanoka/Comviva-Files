package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JavaType;

import com.grameenphone.wipro.extensions.spring.boot.orm.SafeEnumType;
import com.grameenphone.wipro.fmfs.cbp.enums.WorkflowHops;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author wipro.tribhuwan
 *
 */
@Entity
public class WorkflowHop {
	@Id
	@GeneratedValue(
			strategy = GenerationType.AUTO,
			generator = "native"
	)
	@GenericGenerator(
			name = "native"
	)
    private long id;

	@Enumerated(EnumType.STRING)
	@JavaType(WorkflowHops.WorkflowHopsSafeEnum.class)
	private WorkflowHops code;
	
	private String description;
	
	private String displayStatus;

	private boolean autoExecution;
	
	@ManyToOne(fetch = FetchType.LAZY)
    private Action requiredAction;

	private Long clientId;

	@Column(name = "[order]")
	private Integer order;

	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public Long getClientId() {
		return clientId;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public WorkflowHops getCode() {
		return code;
	}

	public void setCode(WorkflowHops code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDisplayStatus() {
		return displayStatus;
	}

	public void setDisplayStatus(String displayStatus) {
		this.displayStatus = displayStatus;
	}

	public boolean isAutoExecution() {
		return autoExecution;
	}

	public void setAutoExecution(boolean autoExecution) {
		this.autoExecution = autoExecution;
	}

	public Action getRequiredAction() {
		return requiredAction;
	}

	public void setRequiredAction(Action action) {
		this.requiredAction = action;
	}
}