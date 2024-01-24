package com.grameenphone.wipro.fmfs.mfs_communicator.pdblink.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pdb.summarydb.scheduler", name = "active")
public class PdbSummarySchedulerTask{
	
	@Autowired
	PdbSummaryScheduler pdbSummaryScheduler;
	
	@Scheduled(cron = "${pdb.summarydb.customerdata.push.cron}")
	public void pushPaidDataTopPDBSft() {
		pdbSummaryScheduler.pushPaidDataTopPDBSft();
	}
	
	@Scheduled(cron = "${pdb.summarydb.billdata.pull.cron}")
	public void pullBillDataFromCSVandLoad() {
		pdbSummaryScheduler.pullBillDataFromCSVandLoad();
	}

	@Scheduled(cron = "${pdb.summarydb.customerdata.pull.cron}")
	public void pullCustomerDataFromCSVandLoad() {
		pdbSummaryScheduler.pullCustomerDataFromCSVandLoad();
	}
	
}