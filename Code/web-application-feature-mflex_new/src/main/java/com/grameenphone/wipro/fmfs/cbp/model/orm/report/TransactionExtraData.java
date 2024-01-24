package com.grameenphone.wipro.fmfs.cbp.model.orm.report;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class TransactionExtraData {
    @Id
    @GeneratedValue(
            strategy= GenerationType.AUTO,
            generator="native"
    )
    @GenericGenerator(
            name = "native",
            strategy = "native"
    )
    public Long id;
    @ManyToOne
    public Header header;
    @ManyToOne
    public Transaction transaction;
    @Column(nullable = false)
    public String value;
}