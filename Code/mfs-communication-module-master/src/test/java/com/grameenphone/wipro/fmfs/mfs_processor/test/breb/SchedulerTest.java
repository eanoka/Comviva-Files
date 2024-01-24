package com.grameenphone.wipro.fmfs.mfs_processor.test.breb;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.DisputeTransactionStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import com.grameenphone.wipro.fmfs.mfs_communicator.config.FlexMFSDbConfig;
import com.grameenphone.wipro.fmfs.mfs_communicator.config.MFSDbConfig;
import com.grameenphone.wipro.fmfs.mfs_communicator.config.MVCConfig;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.BillPayServiceStatusRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.DisputeTransactionRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.scheduler.BillPayDisputeResolverScheduler;
import com.grameenphone.wipro.fmfs.mfs_processor.test.config.TestContextLoader;
import com.grameenphone.wipro.fmfs.mfs_processor.test.config.TestExtension;
import com.grameenphone.wipro.utility.common.MockHttpClient;
import com.grameenphone.wipro.utility.common.MockHttpClient.NotifyCondition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import java.util.List;

import static com.grameenphone.wipro.fmfs.mfs_communicator.service.breb.RebPostpaidService.COMPANY_CODE;

@ExtendWith(TestExtension.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {Application.class, FlexMFSDbConfig.class, MFSDbConfig.class, MVCConfig.class}, loader = TestContextLoader.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@TestInstance(Lifecycle.PER_CLASS)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class SchedulerTest extends RebBillTestBase {
    @Autowired
    DisputeTransactionRepository disputeTransactionRepository;

    @Autowired
    BillPayServiceStatusRepository billPayServiceStatusRepository;

    BillPayDisputeResolverScheduler disputeResolverScheduler;
    @Autowired
    public BeanFactory beanFactory;

    @Value("${test.subscriber.msisdn}")
    protected String subscriberMsisdn;

    @Value("${reb_postpaid_new_token_url}")
    String newTokenUrl;

    @Value("${reb_postpaid_check_txn_status_url}")
    String chkTxnStatusUrl;

    @Value("${reb_postpaid_ack_bill_payment_url}")
    String ackBillPaymentUrl;

    private DisputeTransaction disputeTransaction;
    private final static String DISPUTE_THREAD_NAME = "BILLPAY-SCHEDULER";

    @BeforeAll
    public void initializeDisputeBean() {
        disputeResolverScheduler = new BillPayDisputeResolverScheduler();
        disputeResolverScheduler.beanFactory = beanFactory;
        disputeResolverScheduler.disputeTransactionRepository = disputeTransactionRepository;
    }

    @BeforeEach
    private void populateInitialData() {
        insertInitialToken();

        billAmount = 1200;

        BillPayServiceStatus billPayServiceStatus = new BillPayServiceStatus();
        billPayServiceStatus.setStatus(BillPayStatus.DISPUTE);
        billPayServiceStatus.setAccountNo(TEST_CONSUMER_ID);
        billPayServiceStatus.setAmount(billAmount);
        billPayServiceStatus.setBillNo(TEST_BILL_NO);
        billPayServiceStatus.setCategoryCode("ELEC POST");
        billPayServiceStatus.setChannel("CBP");
        billPayServiceStatus.setCompanyCode(COMPANY_CODE);
        billPayServiceStatus.setMsisdn(subscriberMsisdn);
        billPayServiceStatus.setCreatedBy("TEST");
        billPayServiceStatus.setSessionId("UYRTEJHJSKDF43534YDFJSDHFJSH");
        billPayServiceStatus.setTransactionType("RETBILLPAY");
        billPayServiceStatusRepository.save(billPayServiceStatus);

        disputeTransaction = new DisputeTransaction();
        disputeTransaction.setStatus(DisputeTransactionStatus.PENDING.name());
        disputeTransaction.setAmount(billAmount);
        disputeTransaction.setMeterNo(TEST_CONSUMER_ID);
        disputeTransaction.setBillNo(TEST_BILL_NO);
        disputeTransaction.setBillPayServiceStatus(billPayServiceStatus);
        disputeTransactionRepository.save(disputeTransaction);

        MockHttpClient.reset();
    }

    private String getStatusResponse(int statusCode, int responseCode) {
        return "{\n" +
                "  \"DATA\": " + (responseCode == 1200 ? "{\n" +
                "    \"TRANSACTION_STATUS_CODE\": " + statusCode + ",\n" +
                "    \"TRANSACTION_STATUS_MESSAGE\": \"TRANSACTION NOT EXISTS\"\n" +
                "  }" : "null") + ",\n" +
                "  \"RESPONSE\": {\n" +
                "    \"RESPONSE_MSG\": \"DATA TRANSACTION SUCCESSFULL\",\n" +
                "    \"RESPONSE_CODE\": " + responseCode + "\n" +
                "  }\n" +
                "}";
    }

    public Thread getThreadByName(String threadName) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().equals(threadName)) return t;
        }
        return null;
    }

    @Test
    public void case1() throws Exception {
        logger.debug("Test Case Started: Status check timeout");

        MockHttpClient.forUrl(chkTxnStatusUrl).timeout();

        //Status should be processing
        //Status revert back to pending
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.PENDING.name());
        Assertions.assertEquals(1, transactions.size());

        logger.debug("Test Case Ended: Status check timeout");
    }

    @Test
    public void case4() throws Exception {
        logger.debug("Test Case Started: Status check 1900");

        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(0, 1900));

        //Status should be pending
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.PENDING.name());
        Assertions.assertEquals(1, transactions.size());

        logger.debug("Test Case Ended: Status check 1900");
    }

    @Test
    public void case5() throws Exception {
        logger.debug("Test Case Started: #Status check 1275 #new token garbage");

        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(0, 1275));
        MockHttpClient.forUrl(newTokenUrl).response(200, "GARBAGE RESPONSE");

        //Status should be pending
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.PENDING.name());
        Assertions.assertEquals(1, transactions.size());

        logger.debug("Test Case Ended: #Status check 1275 #new token garbage");
    }

    @Test
    public void case6() throws Exception {
        logger.debug("Test Case Started: #Status check 1275 #new token timeout");

        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(0, 1275));
        MockHttpClient.forUrl(newTokenUrl).timeout();

        //Status should be pending
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.PENDING.name());
        Assertions.assertEquals(1, transactions.size());

        logger.debug("Test Case Ended: #Status check 1275 #new token timeout");
    }

    @Test
    public void case7() throws Exception {
        logger.debug("Test Case Started: #Status check 1275 #new token 1400");

        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(0, 1275));
        MockHttpClient.forUrl(newTokenUrl).response(200, getResponseWithFailCode(1400));

        //Status should be pending
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.PENDING.name());
        Assertions.assertEquals(1, transactions.size());

        logger.debug("Test Case Ended: #Status check 1275 #new token 1400");
    }

    @Test
    public void case8() throws Exception {
        logger.debug("Test Case Started: #Status check 1200 + transaction status 1266");

        mockRollback(false);
        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(1266, 1200));

        //Status should be resolved with bill pay rollback
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.RESOLVED.name());
        Assertions.assertEquals(1, transactions.size());
        Assertions.assertEquals(BillPayStatus.ROLLBACK, transactions.get(0).getBillPayServiceStatus().getStatus());

        logger.debug("Test Case Ended: #Status check 1200 + transaction status 1266");
    }

    @Test
    public void case9() throws Exception {
        logger.debug("Test Case Started: #Status check 1200 + transaction status 1268");

        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(1268, 1200));

        //Status should be resolved with bill pay success
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.RESOLVED.name());
        Assertions.assertEquals(1, transactions.size());
        Assertions.assertEquals(BillPayStatus.SUCCESS, transactions.get(0).getBillPayServiceStatus().getStatus());

        logger.debug("Test Case Ended: #Status check 1200 + transaction status 1266");
    }

    @Test
    public void case10() throws Exception {
        logger.debug("Test Case Started: #Status check 1200 + transaction status 1290");

        mockRollback(false);
        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(1290, 1200));

        //Status should be resolved with bill pay rollback
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.PENDING.name());
        Assertions.assertEquals(1, transactions.size());

        logger.debug("Test Case Ended: #Status check 1200 + transaction status 1290");
    }

    @Test
    public void case11() throws Exception {
        logger.debug("Test Case Started: #Status check 1275 #new token 1200 #Again status check 1275");

        NotifyCondition statusCheckCounter = MockHttpClient.notifyFor(chkTxnStatusUrl);
        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(0, 1275));
        MockHttpClient.forUrl(newTokenUrl).response(200, getNewTokenAPIResponse());

        //Status should be pending
        //Status check should be called twice
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.PENDING.name());
        Assertions.assertEquals(1, transactions.size());
        Assertions.assertEquals(2, statusCheckCounter.getCallCount());

        logger.debug("Test Case Ended: #Status check 1275 #new token 1200 #Again status check 1275");
    }

    @Test
    public void case12() throws Exception {
        logger.debug("Test Case Started: #Status check 1200 + status 1267 #acknowledgement 1250");

        mockRollback(false);
        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(1267, 1200));
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, getResponseWithFailCode(1250));

        //Status should be resolved with bill pay rollback
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.RESOLVED.name());
        Assertions.assertEquals(1, transactions.size());
        Assertions.assertEquals(BillPayStatus.ROLLBACK, transactions.get(0).getBillPayServiceStatus().getStatus());

        logger.debug("Test Case Ended: #Status check 1200 + status 1267 #acknowledgement 1250");
    }

    @Test
    public void case13() throws Exception {
        logger.debug("Test Case Started: #Status check 1200 + status 1267 #acknowledgement 1200");

        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(1267, 1200));
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, getSaveAndAcknowledgementResponse());

        //Status should be resolved with bill pay success
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.RESOLVED.name());
        Assertions.assertEquals(1, transactions.size());
        Assertions.assertEquals(BillPayStatus.SUCCESS, transactions.get(0).getBillPayServiceStatus().getStatus());

        logger.debug("Test Case Ended: #Status check 1200 + status 1267 #acknowledgement 1200");
    }

    @Test
    public void case14() throws Exception {
        logger.debug("Test Case Started: #Status check 1200 + status 1267 #acknowledgement Garbage");

        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(1267, 1200));
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, "GARBAGE RESPONSE");

        //Status should be pending
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.PENDING.name());
        Assertions.assertEquals(1, transactions.size());

        logger.debug("Test Case Ended: #Status check 1200 + status 1267 #acknowledgement 1200");
    }

    @Test
    public void case15() throws Exception {
        logger.debug("Test Case Started: #Status check 1200 + status 1267 #acknowledgement Timeout");

        MockHttpClient.forUrl(chkTxnStatusUrl).response(200, getStatusResponse(1267, 1200));
        MockHttpClient.forUrl(ackBillPaymentUrl).timeout();

        //Status should be pending
        disputeResolverScheduler.resolveDispute();
        Thread thread = getThreadByName(DISPUTE_THREAD_NAME);
        thread.join();
        List<DisputeTransaction> transactions = disputeTransactionRepository.getDisputeTransactionByStatus(DisputeTransactionStatus.PENDING.name());
        Assertions.assertEquals(1, transactions.size());

        logger.debug("Test Case Ended: #Status check 1200 + status 1267 #acknowledgement 1200");
    }

    @AfterEach
    private void clearDisputeTransaction() {
        disputeTransactionRepository.deleteAll();
        billPayServiceStatusRepository.deleteAll();
    }
}