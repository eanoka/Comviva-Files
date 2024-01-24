package com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs;

import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity(name = "api_bill_category_detail")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
public class Category {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	public String categoryCode;
	public String categoryName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}