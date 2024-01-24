package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.util.Collection;
import java.util.Date;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

/**
 * @author wipro.tribhuwan
 */
@Entity
@JsonExcludeNestedProps({"company.category.companies", "clientDivision.client", "addedBy"})
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

    @Column(insertable = false, updatable = false)
    private Date createTime;

    @Column(insertable = false, updatable = false)
    private Date updateTime;

    private String accountNo;

    private int msisdn;

    @ManyToOne(fetch = FetchType.LAZY)
    private ClientDivision clientDivision;

    @ManyToOne(fetch = FetchType.LAZY)
    private User addedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    private User updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    private Company company;

    private String status;
    
    @JoinColumn(name="modified_data_for")
    @OneToOne()
    @Fetch(FetchMode.JOIN)
    private BillData modifiedDataFor;
    
    @Column(name="validated_by_id")
    private Long validatedById;
    
    private String alias;
    
    @OneToMany(mappedBy = "billData")
    private Collection<Bill> bill;
    
    @OneToMany(mappedBy = "billData")
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

    public Date getUpdateTime() {
        return updateTime;
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

    public User getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(User addedBy) {
        this.addedBy = addedBy;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
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

	public BillData getModifiedDataFor() {
		return modifiedDataFor;
	}

	public void setModifiedDataFor(BillData modifiedDataFor) {
		this.modifiedDataFor = modifiedDataFor;
	}

	public Long getValidatedById() {
		return validatedById;
	}

	public void setValidatedById(Long validatedById) {
		this.validatedById = validatedById;
	}

	public Collection<BilldataAdditionalField> getBilldataAddtionalField() {
		return billdataAddtionalField;
	}

	public void setBilldataAddtionalField(Collection<BilldataAdditionalField> billdataAddtionalField) {
		this.billdataAddtionalField = billdataAddtionalField;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Collection<Bill> getBill() {
		return bill;
	}

	public void setBill(Collection<Bill> bill) {
		this.bill = bill;
	}
}