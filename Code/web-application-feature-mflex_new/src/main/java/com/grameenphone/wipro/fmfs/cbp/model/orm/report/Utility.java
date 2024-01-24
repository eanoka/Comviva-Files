package com.grameenphone.wipro.fmfs.cbp.model.orm.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Utility {
    @Id
    public Long id;
    @Column(nullable = false)
    public String name;
    @Column(nullable = false)
    public String code;
    @Column(nullable = false)
    public boolean active;
    @Column(nullable = false)
    public boolean hasUserConstraints;

    @Override
    public int hashCode() {
        return id != null ? id.intValue() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Utility ? id == ((Utility)obj).id : super.equals(obj);
    }
}