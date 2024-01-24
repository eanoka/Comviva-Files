package com.grameenphone.wipro.task_executor.model.orm.cbp;

import java.util.Collection;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.GenericGenerator;

/**
 * @author wipro.tribhuwan
 */
@Entity
public class BillData {
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

    private Date createTime;

    private Date updateTime;

    private String accountNo;

    private String status;

    private int msisdn;

    @ManyToOne(fetch = FetchType.LAZY)
    private ClientDivision clientDivision;

    private int addedById;

    private int updatedById;

    @ManyToOne
    private Company company;
    
    @OneToMany(mappedBy = "billData", fetch = FetchType.EAGER)
    private Collection<BilldataAdditionalField> billdataAddtionalField;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public int getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(int msisdn) {
        this.msisdn = msisdn;
    }

    public ClientDivision getClientDivision() {
        return clientDivision;
    }

    public void setClientDivision(ClientDivision clientDivision) {
        this.clientDivision = clientDivision;
    }

    public int getAddedById() {
        return addedById;
    }

    public void setAddedById(int addedById) {
        this.addedById = addedById;
    }

    public int getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(int updatedById) {
        this.updatedById = updatedById;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

	public Collection<BilldataAdditionalField> getBilldataAddtionalField() {
		return billdataAddtionalField;
	}

	public void setBilldataAddtionalField(Collection<BilldataAdditionalField> billdataAddtionalField) {
		this.billdataAddtionalField = billdataAddtionalField;
	}
   
}