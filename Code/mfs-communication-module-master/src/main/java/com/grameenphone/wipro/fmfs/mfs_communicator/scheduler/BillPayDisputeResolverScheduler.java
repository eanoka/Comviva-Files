package com.grameenphone.wipro.fmfs.mfs_communicator.scheduler;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.DisputeTransactionStatus;
import com.grameenphone.wipro.enums.EventType;
import com.grameenphone.wipro.exception.ServiceProcessingError;
import com.grameenphone.wipro.fmfs.mfs_communicator.event.BillPayEvent;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.DisputeTransactionRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayDisputeResolver;
import com.grameenphone.wipro.utility.common.StringUtil;

@Component
@ConditionalOnProperty(prefix = "bill.pay.dispute.scheduler", name = "active")
public class BillPayDisputeResolverScheduler {
	protected static final Logger logger = LoggerFactory.getLogger(BillPayDisputeResolverScheduler.class);

	@Value("${bill_pay_dispute_scheduler_trigger_interval}")
	String triggerInterval;
	@Value("${bill_pay_dispute_scheduler_initial_delay}")
	String initialDelay;

	@Autowired
	public DisputeTransactionRepository disputeTransactionRepository;
	@Autowired
	public BeanFactory beanFactory;

	@Autowired
	ApplicationEventPublisher appEventPublisher;
	
	@PostConstruct
	public void initMethod() {
		// Setting the property to system as @Scheduled can not take props from external
		// properties file
		System.setProperty("communicator.bill.pay.dispute.scheduler.trigger.interval",
				"" + StringUtil.toMilli(triggerInterval));
		System.setProperty("communicator.bill.pay.dispute.scheduler.initial.delay",
				"" + StringUtil.toMilli(initialDelay));
	}

	@Scheduled(fixedDelayString = "${communicator.bill.pay.dispute.scheduler.trigger.interval}", initialDelayString = "${communicator.bill.pay.dispute.scheduler.initial.delay}")
	public void resolveDispute() {
		new Thread(() -> {
			logger.debug("Starting Bill Pay Dispute Resolver Scheduler ....");
			try {
				Calendar calender = Calendar.getInstance();
				calender.add(Calendar.DATE, -1);
				List<DisputeTransaction> disputeTransactions = disputeTransactionRepository
						.getDisputeTransaction(calender.getTime());
				if (disputeTransactions.size() > 0) {
					logger.debug(disputeTransactions.size() + " Dispute transactions found for resolution");
					for (DisputeTransaction disputeTransaction : disputeTransactions) {
						String companyCode = disputeTransaction.getBillPayServiceStatus().getCompanyCode();
                        if (companyCode.contains(":")) {
                        	companyCode = companyCode.substring(0, companyCode.indexOf(':'));
                		}
						Object executorBean = null;
						try {
							executorBean = beanFactory.getBean(companyCode + "_BillPayDisputeResolverService");
						} catch (Throwable h) {
						}
						if (executorBean instanceof BillPayDisputeResolver) {
							boolean giveUp = false;
							disputeTransaction.setStatus(DisputeTransactionStatus.PROCESSING.name());
							disputeTransactionRepository.save(disputeTransaction);
							try {
								logger.debug("Resolving dispute for: " + disputeTransaction.getMeterNo() + ", bill: "
										+ disputeTransaction.getBillNo());
								((BillPayDisputeResolver) executorBean).resolveDispute(disputeTransaction);
							} catch (ServiceProcessingError k) {
								logger.debug(k.getMessage(), k.getCause());
								if (k.status == 400) {
									giveUp = true;
								}
							} catch (Throwable k) {
								logger.debug("Couldn't resolve dispute for account/meter: "
										+ disputeTransaction.getMeterNo() + ", bill: " + disputeTransaction.getBillNo(),
										k);
							} finally {
								if (giveUp) {
									disputeTransaction.setStatus(DisputeTransactionStatus.GAVE_UP.name());
								} else if (disputeTransaction.getBillPayServiceStatus().getStatus()
										.equals(BillPayStatus.DISPUTE)) {
									if (checkGiveUp(disputeTransaction.getLastUpdateDate())) {
										disputeTransaction.setStatus(DisputeTransactionStatus.GAVE_UP.name());
									} else {
										disputeTransaction.setStatus(DisputeTransactionStatus.PENDING.name());
									}
								} else {
									disputeTransaction.setStatus(DisputeTransactionStatus.RESOLVED.name());
									BillPayServiceStatus billPayData = disputeTransaction.getBillPayServiceStatus();
									if (billPayData.getStatus().equals(BillPayStatus.SUCCESS)) {
										BillPayEvent billPayEvent = new BillPayEvent();
										BillPayEvent.Data data = new BillPayEvent.Data();

										data.amount = billPayData.getAmount();
										data.service_charge = billPayData.getServiceCharge();
										data.utility = billPayData.getCompanyCode();
										data.mfs_txn_id = billPayData.getMfsTxnid();

										billPayEvent.data = data;
										billPayEvent.channel = billPayData.getChannel();
										if (billPayEvent.channel.equals("USSD")) {
											billPayEvent.channel += "_"
													+ billPayData.getTransactionType().substring(0, 3);
										}
										billPayEvent.eventType = EventType.BILLPAY;
										billPayEvent.timestamp = new Date().getTime();
										billPayEvent.initiator = billPayData.getMsisdn();
										billPayEvent.initiator_on_behalf = billPayData.getCustomerMsisdn();

										appEventPublisher.publishEvent(billPayEvent);
									}
								}
								disputeTransactionRepository.save(disputeTransaction);
							}
						}
					}
				} else {
					logger.debug("No dispute transaction found.");
				}
			} catch (Exception e) {
				logger.error("Error fetching dispute transaction list:: ", e);
			} finally {
				logger.debug("Closing Bill Pay Dispute Resolver Scheduler ....");
			}
		}, "BILLPAY-SCHEDULER").start();
	}

	private boolean checkGiveUp(Date lastUpdatedDate) {
		int interval = Integer.parseInt(triggerInterval.replace("m", ""));
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.HOUR, -24);
		cal.add(Calendar.MINUTE, +(interval + 2));
		Date giveUpBefore = cal.getTime();
		return lastUpdatedDate.before(giveUpBefore);
	}

}
