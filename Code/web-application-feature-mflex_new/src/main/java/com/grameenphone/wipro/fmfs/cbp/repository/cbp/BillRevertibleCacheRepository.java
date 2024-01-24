package com.grameenphone.wipro.fmfs.cbp.repository.cbp;

import org.springframework.data.repository.CrudRepository;

import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Bill;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillRevertibleCache;

public interface BillRevertibleCacheRepository extends CrudRepository<BillRevertibleCache, Long> {
	Long deleteByBill(Bill bill);
}
