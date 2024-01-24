package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.util.Collection;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

/**
 * @author wipro.tribhuwan
 */
@Entity
public class Category {
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
    
    private String name;
    
    private String code;

	@OneToMany
	@JoinColumn(name = "category_id")
    private Collection<Company> companies;

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Collection<Company> getCompanies() {
		return companies;
	}

	public void setCompanies(Collection<Company> companies) {
		this.companies = companies;
	}
}