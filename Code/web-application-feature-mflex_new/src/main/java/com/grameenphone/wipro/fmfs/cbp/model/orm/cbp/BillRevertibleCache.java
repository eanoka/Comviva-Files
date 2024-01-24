package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * @author wipro.tribhuwan
 */
@Entity
public class BillRevertibleCache {
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

    @OneToOne
    private Bill bill;
    private String valuesAsJson;
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public Bill getBill() {
		return bill;
	}
	public void setBill(Bill bill) {
		this.bill = bill;
	}
	public String getValuesAsJson() {
		return valuesAsJson;
	}
	public void setValuesAsJson(String valuesAsJson) {
		this.valuesAsJson = valuesAsJson;
	}
}