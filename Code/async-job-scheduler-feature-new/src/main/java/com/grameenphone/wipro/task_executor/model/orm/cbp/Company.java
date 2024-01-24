package com.grameenphone.wipro.task_executor.model.orm.cbp;

import java.util.Collection;

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
 *
 */
@Entity
public class Company {
	@Id
    @GeneratedValue(
            strategy = GenerationType.AUTO,
            generator = "native"
    )
    @GenericGenerator(
            name = "native",
            strategy = "native"
    )
    private int id;
    
    private String code;
	
    private String name;
    
    private Boolean hasBill;
    
    private String billRevertibles;

    @ManyToOne
    private Category category;
    
    @OneToMany(mappedBy = "company")
    private Collection<CompanyAdditionalFields> fields;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isHasBill() {
        return hasBill;
    }

    public void setHasBill(Boolean hasBill) {
        this.hasBill = hasBill;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getBillRevertibles() {
        return billRevertibles;
    }

    public void setBillRevertibles(String billRevertibles) {
        this.billRevertibles = billRevertibles;
    }

	public Collection<CompanyAdditionalFields> getFields() {
		return fields;
	}

	public void setFields(Collection<CompanyAdditionalFields> fields) {
		this.fields = fields;
	}
    
}