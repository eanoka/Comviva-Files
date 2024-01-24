package com.grameenphone.wipro.fmfs.cbp.repository.report;

import com.grameenphone.wipro.fmfs.cbp.model.orm.report.Transaction;
import org.springframework.data.repository.CrudRepository;

public interface TransactionRepository extends CrudRepository<Transaction, Long> {
}