package com.grameenphone.wipro.task_executor.model.orm.cbp;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

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
            name = "native"
    )
    private long id;

    @OneToOne
    private Bill bill;
    private String valuesAsJson;
}