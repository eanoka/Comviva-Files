package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.io.Serializable;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * @author wipro.tribhuwan
 */
@Entity
public class Action implements Serializable {
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
    private Long clientId;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Action ? getId() != 0 ? getId() == ((Action) obj).getId() : super.equals(obj) : super.equals(obj);
    }

    @Override
    public int hashCode() {
        return getId() != 0 ? (int)getId() : super.hashCode();
    }
}