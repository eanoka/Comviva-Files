package com.grameenphone.wipro.fmfs.cbp.repository.cbp;

import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillData;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillDetailTask;
import org.springframework.data.repository.CrudRepository;

public interface BillDataRepository extends CrudRepository<BillData, Long> {
}