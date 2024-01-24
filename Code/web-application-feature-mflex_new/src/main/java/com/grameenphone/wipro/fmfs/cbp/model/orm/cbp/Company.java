package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.util.Collection;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

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
    private long id;
    
    private String code;
	
    private String name;
    
    private Boolean hasBill;

    @ManyToOne
    private Category category;
    
    @OneToMany(mappedBy = "company")
    private Collection<CompanyAdditionalFields> fields;

    public long getId() {
        return id;
    }

    public void setId(long id) {
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
    
    public Collection<CompanyAdditionalFields> getFields() {
		return fields;
	}

	public void setFields(Collection<CompanyAdditionalFields> fields) {
		this.fields = fields;
	}

	@Override
    public boolean equals(Object obj) {
        return obj instanceof Company && getId() != 0 && ((Company)obj).getId() == getId();
    }

    @Override
    public int hashCode() {
        return (int)id;
    }
}