package com.grameenphone.wipro.task_executor.model.orm.cbp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;

@Entity
public class BilldataAdditionalField 
{
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
	
	private String value;
	
	@ManyToOne
	@JoinColumn(name = "field_id")
	private CompanyAdditionalFields fields;
	
	@ManyToOne
	@JoinColumn(name = "billdata_id")
	private BillData billData;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public CompanyAdditionalFields getFields() {
		return fields;
	}

	public void setFields(CompanyAdditionalFields fields) {
		this.fields = fields;
	}

	public BillData getBillData() {
		return billData;
	}

	public void setBillData(BillData billData) {
		this.billData = billData;
	}	
	
}
