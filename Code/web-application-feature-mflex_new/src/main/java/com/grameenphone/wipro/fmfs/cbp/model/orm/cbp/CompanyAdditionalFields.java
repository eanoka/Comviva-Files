package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.util.Collection;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class CompanyAdditionalFields 
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
	
	private String paramCode;
	
	private String paramName;
	
	private String paramType;
	
	private String paramLength;
	
	private String paramValues;
	
	private String validationRegex;
	
	private String required;
	
	private String errorMessage;
	
	private String status;
	
	private String configs;
	
	private boolean isBilldataInput;
	
	@ManyToOne
	@JoinColumn(name = "bill_company_detail_id")
	private Company company;
	
	@OneToMany(mappedBy = "fields")
	private Collection<BilldataAdditionalField> bill;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getParamCode() {
		return paramCode;
	}

	public void setParamCode(String paramCode) {
		this.paramCode = paramCode;
	}

	public String getParamName() {
		return paramName;
	}

	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	public String getParamType() {
		return paramType;
	}

	public void setParamType(String paramType) {
		this.paramType = paramType;
	}

	public String getParamLength() {
		return paramLength;
	}

	public void setParamLength(String paramLength) {
		this.paramLength = paramLength;
	}

	public String getParamValues() {
		return paramValues;
	}

	public void setParamValues(String paramValues) {
		this.paramValues = paramValues;
	}

	public String getValidationRegex() {
		return validationRegex;
	}

	public void setValidationRegex(String validationRegex) {
		this.validationRegex = validationRegex;
	}

	public String getRequired() {
		return required;
	}

	public void setRequired(String required) {
		this.required = required;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getConfigs() {
		return configs;
	}

	public void setConfigs(String configs) {
		this.configs = configs;
	}

	public boolean isBilldataInput() {
		return isBilldataInput;
	}

	public void setBilldataInput(boolean isBilldataInput) {
		this.isBilldataInput = isBilldataInput;
	}

	public Collection<BilldataAdditionalField> getBill() {
		return bill;
	}

	public void setBill(Collection<BilldataAdditionalField> bill) {
		this.bill = bill;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

}
