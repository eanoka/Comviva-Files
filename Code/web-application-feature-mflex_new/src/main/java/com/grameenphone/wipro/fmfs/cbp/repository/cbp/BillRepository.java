package com.grameenphone.wipro.fmfs.cbp.repository.cbp;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Bill;

public interface BillRepository extends CrudRepository<Bill, Long> {
	public List<Bill> findByRequestId(long id);
}