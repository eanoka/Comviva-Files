package com.grameenphone.wipro.task_executor.processors;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.grameenphone.wipro.task_executor.Main;
import com.grameenphone.wipro.task_executor.dao.GetBillDao;
import com.grameenphone.wipro.task_executor.dao.UserDao;
import com.grameenphone.wipro.task_executor.enums.BillPaymentStatus;
import com.grameenphone.wipro.task_executor.enums.BillStatus;
import com.grameenphone.wipro.task_executor.enums.TaskStatus;
import com.grameenphone.wipro.task_executor.model.api.PayBillRequest;
import com.grameenphone.wipro.task_executor.model.api.PayBillResponse;
import com.grameenphone.wipro.task_executor.model.entity.PaymentRequest;
import com.grameenphone.wipro.task_executor.model.entity.PaymentTask;
import com.grameenphone.wipro.task_executor.model.entity.User;
import com.grameenphone.wipro.task_executor.model.orm.cbp.Bill;
import com.grameenphone.wipro.task_executor.service.PayBillService;
import com.grameenphone.wipro.task_executor.util.KV;
import com.grameenphone.wipro.task_executor.util.MacroReplacer;
import com.grameenphone.wipro.task_executor.util.PropertyUtil;

public class BillPayer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(BillPayer.class);
    private PayBillService payBillService = Main.cbpContext.getBean(PayBillService.class);
    private UserDao userDao = new UserDao();
    private int success = 0;
    private int fail = 0;
    private int dispute = 0;
    private int totalProcessed = 0;
    
    @Value("${task_executor_node_id}")
    private String instanceId;

    @Autowired
    private GetBillDao getBillDao;

    @Override
    public void run() {
        logger.debug("Starting pay bill thread.");
        PaymentTask paymentTask;
        long startTime = 0;
        long endTime;
        long completedHopId = payBillService.getWorkflowHopId("CPL");
        long rejectedHopId = payBillService.getWorkflowHopId("REJ");
        while ((paymentTask = payBillService.getPaymentTask()) != null) {
            try {
                startTime = System.currentTimeMillis();
                long nextHopId;
                totalProcessed = 0;
                success = 0;
                fail = 0;
                dispute = 0;
                if (paymentTask.pin != null && !paymentTask.pin.isEmpty()) {
                    if (!payBillService.tryUpdateStatusToProcessing(paymentTask)) {
                        continue;
                    }
                    logger.debug("Found payment task Id: " + paymentTask.id);
                    List<PayBillRequest> payBillRequests = payBillService.collectPendingBills(paymentTask.requestHopId);
                    logger.debug("Total " + payBillRequests.size() + " Bill found for payment task: " + paymentTask.id);
                    for (PayBillRequest payBillRequest : payBillRequests) {
                        String errorMessage = null;
                        PayBillResponse payBillResponse = null;
                        boolean abortRequest = false;
                        try {
                            payBillRequest.pin = paymentTask.pin;
                            totalProcessed++;
                            payBillResponse = payBillService.payBill(payBillRequest);
                            
                            if(payBillResponse.status==422) {
                                fail++;
                                errorMessage = payBillResponse.message;
                                if(!payBillRequest.hasBill) 
                                {
                                	saveBillAndBillRequestDetail(payBillRequest, errorMessage, payBillResponse.response.txnId);
                                }
                                
							} else if (payBillResponse.status == 200) {
								if (payBillResponse.response.status.equals(BillStatus.Success.name())) {
									success++;
								} else if (payBillResponse.response.status.equals(BillStatus.Dispute.name())) {
									dispute++;
								} else {
									errorMessage = payBillResponse.response.message;
									fail++;
									if(!payBillRequest.hasBill) 
	                                {
	                                	saveBillAndBillRequestDetail(payBillRequest, errorMessage, payBillResponse.response.txnId);
	                                }
									switch (payBillResponse.response.failCode) {
									case "00210":
									case "00068":
									case "00099":
										abortRequest = true;
										break;
									}
								}
							} else {
								dispute++;
								errorMessage = payBillResponse.message;
							}
                        } catch (Exception e) {
                            logger.error("Error occurred while paying bill for: " + payBillRequest.bill, e);
                            dispute++;
                        }
                        if(payBillResponse == null) {
                            payBillService.updateBillStatus(BillStatus.Dispute.name(), payBillRequest.consumerId, payBillRequest.bill, payBillRequest.companyId, null, null, null);
                        } else {
                            payBillService.updateBillStatus(payBillResponse.response == null ? BillPaymentStatus.Fail.name():getBillStatus(payBillResponse.response.status), payBillRequest.consumerId, payBillRequest.bill, payBillRequest.companyId, payBillResponse.response == null ? null:payBillResponse.response.txnId, payBillResponse.timestamp, errorMessage);
                        }
                        if(abortRequest) {
                            break;
                        }
                    }
                    nextHopId = completedHopId;
                } else {
                    logger.debug("Unable to get pin for TaskId: " + paymentTask.id);
                    nextHopId = rejectedHopId;
                }
                payBillService.updateHopIdInPaymentRequest(payBillService.insertAndUpdatePaymentRequestHop(paymentTask.requestHopId, nextHopId));
            } catch (Exception e) {
                logger.error("Exception occured while paying bill:: ", e);
            } finally {
                endTime = System.currentTimeMillis();
                paymentTask.startTime = new Timestamp(startTime);
                paymentTask.endTime = new Timestamp(endTime);
                paymentTask.totalProcessed = totalProcessed;
                paymentTask.successCount = success;
                paymentTask.disputeCount = dispute;
                paymentTask.failedCount = fail;
                paymentTask.status = TaskStatus.Completed;
                payBillService.updatePaymentTask(paymentTask);
                PaymentRequest request = payBillService.getPaymentRequest(paymentTask.requestHopId);
                payBillService.sendWebNotification(request.id);
                paymentTask.creationTime = request.createTime;
                paymentTask.requestId = request.id;
                sendMail(paymentTask);
            }
        }
    }

	private String getBillStatus(String mflexStatus) {
        switch(mflexStatus) {
            case "Success":
                return BillStatus.Success.name();
            case "Fail":
            case "Rollback":
            case "Rollback Fail":
                return BillStatus.Fail.name();
            case "Dispute":
            default:
                return BillStatus.Dispute.name();
        }
    }

    private void sendMail(PaymentTask paymentTask) {
        String tempMailBody = PropertyUtil.getProperty("email-template-pay-bill");
        String mailSubject = PropertyUtil.getProperty("subject-template-pay-bill");
        User user = userDao.findByRequestHopId(paymentTask.requestHopId);
        String modifiedMailBody = null;
        try {
            modifiedMailBody = MacroReplacer.replaceMacros(tempMailBody, new KV<>("user", user), new KV<>("task", paymentTask), new KV<>("request_id", "PR" + String.format("%08x", paymentTask.requestId).toUpperCase()));
        } catch (Exception e) {
            logger.error("Macro replacing Error::" + e.getMessage());
        }
        Main.notificationSenderExecutors.submit(new MailThread(modifiedMailBody, user.emailAddress, "", mailSubject));
    }
    
    @Transactional
    private void saveBillAndBillRequestDetail(PayBillRequest payBillRequest, String errorMessage, String txnId) {
    	Bill bill = getBillDao.findBillDetail(payBillRequest.consumerId, payBillRequest.bill);
    	insertBillRequest(bill.getId(), bill.getRequestId(), "UNPAID", null, errorMessage);
    	insertBill(bill.getClientDivision().getId(), bill.getCompany().getId(), bill.getAccountNo(), bill.getBillAmount(), bill.getServiceCharge(), bill.getVat(), instanceId + new SimpleDateFormat("yyMMddHHmmssSSS").format(new Date()) + ((System.nanoTime() % 1000000) / 1000), bill.getBillDataId(), String.valueOf(bill.getMsisdn()), new Timestamp(bill.getDueDate().getTime()));
	}
    
    public boolean insertBill(long clientDivisionId, int companyId, String consumerId, Double amount, Double serviceCharge, Double vat, String billNo, long billDataId, String msisdn, Timestamp dueDate) {
        return getBillDao.insertBill((int)clientDivisionId, companyId, consumerId, amount, serviceCharge, vat, billNo, (int)billDataId, msisdn, dueDate);
    }
    
    public boolean insertBillRequest(long billId, String requestId, String status, String mfsTxnId, String errorMessage) {
        return getBillDao.insertBillRequest(billId, requestId, status, mfsTxnId, errorMessage);
    }
}