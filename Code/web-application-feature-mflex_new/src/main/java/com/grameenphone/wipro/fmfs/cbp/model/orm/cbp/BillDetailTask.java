package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JavaType;

import com.grameenphone.wipro.fmfs.cbp.enums.TaskStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * @author wipro.tribhuwan
 *
 */
@Entity
public class BillDetailTask {
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

    @Enumerated(EnumType.STRING)
    @JavaType(TaskStatus.TaskStatusSafeEnum.class)
    private TaskStatus status = TaskStatus.Pending;

    private String accountNo;

    @OneToMany
    private Collection<ClientDivision> clientDivisions = new ArrayList<>();
      
    private String nodeId;
    private int totalProcessed;
    private int successCount;
    private int failedCount;
    private Date startTime;
    private Date endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    private User addedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getTotalProcessed() {
        return totalProcessed;
    }

    public void setTotalProcessed(int totalProcessed) {
        this.totalProcessed = totalProcessed;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
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

    public User getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(User addedBy) {
        this.addedBy = addedBy;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Collection<ClientDivision> getClientDivisions() {
        return clientDivisions;
    }

    public void setClientDivisions(Collection<ClientDivision> clientDivisions) {
        this.clientDivisions = clientDivisions;
    }

    public void addClientDivisions(ClientDivision clientDivisions) {
        this.clientDivisions.add(clientDivisions);
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}