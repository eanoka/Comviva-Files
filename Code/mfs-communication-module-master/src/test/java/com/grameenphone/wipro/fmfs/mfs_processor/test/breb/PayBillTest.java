package com.grameenphone.wipro.fmfs.mfs_processor.test.breb;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import com.grameenphone.wipro.fmfs.mfs_communicator.config.FlexMFSDbConfig;
import com.grameenphone.wipro.fmfs.mfs_communicator.config.MFSDbConfig;
import com.grameenphone.wipro.fmfs.mfs_communicator.config.MVCConfig;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.DisputeTransaction;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.*;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.breb.RebPostpaidService;
import com.grameenphone.wipro.fmfs.mfs_processor.test.config.TestContextLoader;
import com.grameenphone.wipro.fmfs.mfs_processor.test.config.TestExtension;
import com.grameenphone.wipro.utility.common.MockHttpClient;
import com.grameenphone.wipro.utility.common.MockHttpClient.NotifyCondition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.ResultActions;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(TestExtension.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {Application.class, FlexMFSDbConfig.class, MFSDbConfig.class, MVCConfig.class}, loader = TestContextLoader.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@TestInstance(Lifecycle.PER_CLASS)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class PayBillTest extends RebBillTestBase {
    @Autowired
    DisputeTransactionRepository disputeTransactionRepository;

    @Autowired
    BillPayServiceStatusRepository billPayServiceStatusRepository;

    @Autowired
    PrepaidBillTokenRepository prepaidBillTokenRepository;

    @Value("${test.subscriber.additional.msisdn}")
    protected String subscriberAdditionalMsisdn;

    @Value("${test.subscriber.additional.pin}")
    protected String subscriberWalletAdditionalPin;

    @Value("${reb_postpaid_ack_bill_payment_url}")
    String ackBillPaymentUrl;

    @Value("${reb_postpaid_save_bill_query_url}")
    String billSaveUrl;

    public Map<String, Object> getPayRequestParams() {
        return new LinkedHashMap<>() {{
            put("BILL_MONTH", "03");
            put("BILL_YEAR", "2021");
        }};
    }

    @BeforeEach
    public void clearHttpMocks() {
        insertInitialToken();
        mockAssociation(true);
        MockHttpClient.reset();
    }

    private ResultActions callPayment(boolean isSubscriber) throws Exception {
        return mockMvc.perform(post("/bill/payment").contentType(MediaType.APPLICATION_JSON).content(getPayBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID, isSubscriber, getPayRequestParams())));
    }

    @Test
    public void case1() throws Exception {
        String temp1 = subscriberMsisdn;
        String temp2 = subscriberWalletPin;
        subscriberMsisdn = subscriberAdditionalMsisdn;
        subscriberWalletPin = subscriberWalletAdditionalPin;
        logger.debug("Test Case Started: #Not Associated #Wallet Deduction Failed");

        try {
            NotifyCondition associationCallCounter = notifyAssociation(true);
            mockWalletDeduction(true, false);

            //Association API should be called
            //Deduction Fail Error Message
            callPayment(true).andExpect(jsonPath("response.message").value(mfsErrorMessage));
            Assertions.assertEquals(1, associationCallCounter.getCallCount(), "Association API Call Count not matching");
        } finally {
            subscriberMsisdn = temp1;
            subscriberWalletPin = temp2;
        }

        logger.debug("Test Case Ended: #Not Associated #Wallet Deduction Failed");
    }

    @Test
    public void case2() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill API Garbage Response");

        NotifyCondition associationCallCounter = notifyAssociation(true);
        mockWalletDeduction(false, true);
        MockHttpClient.forUrl(billSaveUrl).response(200, "GARBAGE RESPONSE");

        //Association API not called
        //Dispute Entry In Table
        //Billpay status will be Dispute
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.DISPUTE));
        Assertions.assertEquals(0, associationCallCounter.getCallCount(), "Association API Call Count not matching");
        DisputeTransaction disputeTransaction = disputeTransactionRepository.getLastPendingDisputeTransaction(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertNotNull(disputeTransaction);
        Assertions.assertEquals(disputeTransaction.getAmount(), billAmount);
        Assertions.assertEquals(disputeTransaction.getBillPayServiceStatus().getStatus(), BillPayStatus.DISPUTE);

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill API Garbage Response");
    }

    @Test
    public void case3() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill API Timeout");

        mockWalletDeduction(false, true);
        MockHttpClient.forUrl(billSaveUrl).timeout();

        //Dispute Entry In Table
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.DISPUTE));
        DisputeTransaction disputeTransaction = disputeTransactionRepository.getLastPendingDisputeTransaction(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertNotNull(disputeTransaction);

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill API Timeout");
    }

    @Test
    public void case4() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill Not 1200 or 1275");

        NotifyCondition rollbackApiNotify = MockHttpClient.notifyFor(mfsApiPath + "&REQUEST_GATEWAY_TYPE=USSD").isPost().content("REQTRCORIN", false);
        mockWalletDeduction(true, true);
        mockRollback(true);
        MockHttpClient.forUrl(billSaveUrl).response(200, getResponseWithFailCode(1500));

        //Rollback API Called
        //Status changed to Rollback
        callPayment(true).andExpect(jsonPath("response.status").value(BillPayStatus.ROLLBACK));
        Assertions.assertEquals(1, rollbackApiNotify.getCallCount());
        BillPayServiceStatus billPayServiceStatus = billPayServiceStatusRepository.getLastBillPayServiceStatusByCompanyCodeAndAccountNo(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertEquals(BillPayStatus.ROLLBACK, billPayServiceStatus.getStatus());

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill Not 1200 or 1275");
    }

    @Test
    public void case5() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement Garbage Response");

        mockWalletDeduction(false, true);
        MockHttpClient.forUrl(billSaveUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, "GARBAGE RESPONSE");

        //Dispute Entry In Table
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.DISPUTE));
        DisputeTransaction disputeTransaction = disputeTransactionRepository.getLastPendingDisputeTransaction(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertNotNull(disputeTransaction);

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement Garbage Response");
    }

    @Test
    public void case5a() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Access Invalid #Refresh Valid #Acknowledgement Garbage Response");

        String tokenResponse = getNewTokenAPIResponse();
        String authResponse = getAuthenticationAPIResponse();
        mockWalletDeduction(false, true);
        MockHttpClient.forUrl(billSaveUrl).callCount(1).response(200, getResponseWithFailCode(1271));
        MockHttpClient.forUrl(billSaveUrl).callCount(2).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, "GARBAGE RESPONSE");
        MockHttpClient.forUrl(newTokenUrl).response(200, tokenResponse, "UTF-8");
        MockHttpClient.forUrl(authenticationUrl).response(200, authResponse, "UTF-8");
        MockHttpClient.NotifyCondition tokenNotifier = MockHttpClient.notifyFor(newTokenUrl);
        MockHttpClient.NotifyCondition authNotifier = MockHttpClient.notifyFor(authenticationUrl);

        //Dispute Entry In Table
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.DISPUTE));
        DisputeTransaction disputeTransaction = disputeTransactionRepository.getLastPendingDisputeTransaction(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertNotNull(disputeTransaction);
        Assertions.assertEquals(true, tokenNotifier.isCalled());
        Assertions.assertEquals(false, authNotifier.isCalled());

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement Garbage Response");
    }

    @Test
    public void case5b() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement Garbage Response");

        String tokenResponse = getResponseWithFailCode(1273);;
        String authResponse = getAuthenticationAPIResponse();
        mockWalletDeduction(false, true);
        MockHttpClient.forUrl(billSaveUrl).callCount(1).response(200, getResponseWithFailCode(1271));
        MockHttpClient.forUrl(billSaveUrl).callCount(2).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, "GARBAGE RESPONSE");
        MockHttpClient.forUrl(newTokenUrl).response(200, tokenResponse, "UTF-8");
        MockHttpClient.forUrl(authenticationUrl).response(200, authResponse, "UTF-8");
        MockHttpClient.NotifyCondition tokenNotifier = MockHttpClient.notifyFor(newTokenUrl);
        MockHttpClient.NotifyCondition authNotifier = MockHttpClient.notifyFor(authenticationUrl);

        //Dispute Entry In Table
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.DISPUTE));
        DisputeTransaction disputeTransaction = disputeTransactionRepository.getLastPendingDisputeTransaction(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertNotNull(disputeTransaction);
        Assertions.assertEquals(true, tokenNotifier.isCalled());
        Assertions.assertEquals(true, authNotifier.isCalled());

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement Garbage Response");
    }

    @Test
    public void case6() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement Timeout");

        mockWalletDeduction(true, true);
        MockHttpClient.forUrl(billSaveUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).timeout();

        //Dispute Entry In Table
        callPayment(true).andExpect(jsonPath("response.status").value(BillPayStatus.DISPUTE));
        DisputeTransaction disputeTransaction = disputeTransactionRepository.getLastPendingDisputeTransaction(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertNotNull(disputeTransaction);

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement Timeout");
    }

    @Test
    public void case7() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement Not 1200 or 1275");

        mockWalletDeduction(false, true);
        mockRollback(false);
        MockHttpClient.forUrl(billSaveUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, getResponseWithFailCode(1500));

        //Rollback API Called
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.ROLLBACK));
        BillPayServiceStatus billPayServiceStatus = billPayServiceStatusRepository.getLastBillPayServiceStatusByCompanyCodeAndAccountNo(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertNotNull(billPayServiceStatus.getAttr4());

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement Not 1200 or 1275");
    }

    @Test
    public void case8() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1200");

        mockWalletDeduction(true, true);
        MockHttpClient.forUrl(billSaveUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, getSaveAndAcknowledgementResponse());

        //Billpay status Success
        callPayment(true).andExpect(jsonPath("response.status").value(BillPayStatus.SUCCESS)).andExpect(jsonPath("response.txnId").isNotEmpty());
        BillPayServiceStatus billPayServiceStatus = billPayServiceStatusRepository.getLastBillPayServiceStatusByCompanyCodeAndAccountNo(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertEquals(BillPayStatus.SUCCESS, billPayServiceStatus.getStatus());

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1200");
    }

    @Test
    public void case8b() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1200 #Access Invalid #Refresh Invalid");

        String invalidTokenResponse = getResponseWithFailCode(1271);
        String tokenResponse = getResponseWithFailCode(1273);
        String authResponse = getAuthenticationAPIResponse();
        mockWalletDeduction(true, true);
        MockHttpClient.forUrl(billSaveUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).callCount(1).response(200, invalidTokenResponse);
        MockHttpClient.forUrl(ackBillPaymentUrl).callCount(2).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(newTokenUrl).response(200, tokenResponse, "UTF-8");
        MockHttpClient.forUrl(authenticationUrl).response(200, authResponse, "UTF-8");
        MockHttpClient.NotifyCondition tokenNotifier = MockHttpClient.notifyFor(newTokenUrl);
        MockHttpClient.NotifyCondition authNotifier = MockHttpClient.notifyFor(authenticationUrl);

        //Billpay status Success
        callPayment(true).andExpect(jsonPath("response.status").value(BillPayStatus.SUCCESS)).andExpect(jsonPath("response.txnId").isNotEmpty());
        BillPayServiceStatus billPayServiceStatus = billPayServiceStatusRepository.getLastBillPayServiceStatusByCompanyCodeAndAccountNo(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertEquals(BillPayStatus.SUCCESS, billPayServiceStatus.getStatus());
        Assertions.assertEquals(true, tokenNotifier.isCalled());
        Assertions.assertEquals(true, authNotifier.isCalled());

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1200");
    }

    @Test
    public void case9() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1275 #New Token Garbage");

        mockWalletDeduction(false, true);
        MockHttpClient.forUrl(billSaveUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, getResponseWithFailCode(1275));
        NotifyCondition newTokenApiCount = MockHttpClient.notifyFor(newTokenUrl);
        MockHttpClient.forUrl(newTokenUrl).response(200, "GARBAGE RESPONSE");

        //Dispute Entry In Table
        //New Token API Called
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.DISPUTE));
        Assertions.assertEquals(1, newTokenApiCount.getCallCount());
        DisputeTransaction disputeTransaction = disputeTransactionRepository.getLastPendingDisputeTransaction(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertNotNull(disputeTransaction);

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1275 #New Token Garbage");
    }

    @Test
    public void case10() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1275 #New Token Timeout");

        mockWalletDeduction(true, true);
        MockHttpClient.forUrl(billSaveUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, getResponseWithFailCode(1275));
        MockHttpClient.forUrl(newTokenUrl).timeout();

        //Dispute Entry In Table
        callPayment(true).andExpect(jsonPath("response.status").value(BillPayStatus.DISPUTE));
        DisputeTransaction disputeTransaction = disputeTransactionRepository.getLastPendingDisputeTransaction(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
        Assertions.assertNotNull(disputeTransaction);

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1275 #New Token Timeout");
    }

    @Test
    public void case11() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1275 #New Token 1200 #Second Acknowledgement 1200");

        mockWalletDeduction(false, true);
        MockHttpClient.forUrl(billSaveUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).callCount(1).response(200, getResponseWithFailCode(1275));
        MockHttpClient.forUrl(newTokenUrl).response(200, getNewTokenAPIResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).callCount(2).response(200, getSaveAndAcknowledgementResponse());
        NotifyCondition acknowledgementCount = MockHttpClient.notifyFor(ackBillPaymentUrl);
        MockHttpClient.NotifyCondition tokenNotifier = MockHttpClient.notifyFor(newTokenUrl);
        MockHttpClient.NotifyCondition authNotifier = MockHttpClient.notifyFor(authenticationUrl);

        //Acknowledgement been called twice
        //Bill pay status success
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.SUCCESS));
        Assertions.assertEquals(2, acknowledgementCount.getCallCount());
        Assertions.assertEquals(true, tokenNotifier.isCalled());
        Assertions.assertEquals(false, authNotifier.isCalled());

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1275 #New Token 1200 #Second Acknowledgement 1200");
    }

    @Test
    public void case12() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1275 #New Token 1200 #Second Acknowledgement 1275");

        mockWalletDeduction(true, true);
        mockRollback(true);
        MockHttpClient.forUrl(billSaveUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, getResponseWithFailCode(1275));
        MockHttpClient.forUrl(newTokenUrl).response(200, getNewTokenAPIResponse());
        NotifyCondition acknowledgementCount = MockHttpClient.notifyFor(ackBillPaymentUrl);
        NotifyCondition acknowledgementCountWithNewToken = MockHttpClient.notifyFor(ackBillPaymentUrl).content(renewedAccessTokenFromNewToken, false);

        //Acknowledgement been called twice
        //Transaction status is rollback
        callPayment(true).andExpect(jsonPath("response.status").value(BillPayStatus.ROLLBACK));
        Assertions.assertEquals(2, acknowledgementCount.getCallCount());
        Assertions.assertEquals(1, acknowledgementCountWithNewToken.getCallCount());

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1275 #New Token 1200 #Second Acknowledgement 1275");
    }

    @Test
    public void case13() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1275 #New Token not 1200");

        mockWalletDeduction(false, true);
        mockRollback(false);
        MockHttpClient.forUrl(billSaveUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, getResponseWithFailCode(1275));
        MockHttpClient.forUrl(newTokenUrl).response(200, getResponseWithFailCode(1275));

        //Transaction status is rollback
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.DISPUTE));

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1200 #Acknowledgement 1275 #New Token not 1200");
    }

    @Test
    public void case14() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1275 #New Token Garbage");

        mockWalletDeduction(true, true);
        mockRollback(true);
        MockHttpClient.forUrl(billSaveUrl).response(200, getResponseWithFailCode(1275));
        MockHttpClient.forUrl(newTokenUrl).response(200, "GARBAGE RESPONSE");

        //Transaction status is rollback
        callPayment(true).andExpect(jsonPath("response.status").value(BillPayStatus.ROLLBACK));

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1275 #New Token Garbage");
    }

    @Test
    public void case15() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1275 #New Token Timeout");

        mockWalletDeduction(false, true);
        mockRollback(false);
        MockHttpClient.forUrl(billSaveUrl).response(200, getResponseWithFailCode(1275));
        MockHttpClient.forUrl(newTokenUrl).timeout();

        //Transaction status is rollback
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.ROLLBACK));

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1275 #New Token Timeout");
    }

    @Test
    public void case16() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1275 #New Token 1200 #Second Save Bill 1200 #Acknowledgement 1200");

        mockWalletDeduction(true, true);
        MockHttpClient.forUrl(billSaveUrl).callCount(1).response(200, getResponseWithFailCode(1275));
        MockHttpClient.forUrl(newTokenUrl).response(200, getNewTokenAPIResponse());
        MockHttpClient.forUrl(billSaveUrl).callCount(2).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.forUrl(ackBillPaymentUrl).response(200, getSaveAndAcknowledgementResponse());
        MockHttpClient.NotifyCondition tokenNotifier = MockHttpClient.notifyFor(newTokenUrl);
        MockHttpClient.NotifyCondition authNotifier = MockHttpClient.notifyFor(authenticationUrl);

        //Billpay status Success
        callPayment(true).andExpect(jsonPath("response.status").value(BillPayStatus.SUCCESS));
        Assertions.assertEquals(true, tokenNotifier.isCalled());
        Assertions.assertEquals(false, authNotifier.isCalled());

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1275 #New Token 1200 #Second Save Bill 1200 #Acknowledgement 1200");
    }

    @Test
    public void case17() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1275 #New Token 1200 #Second Save Bill 1275");

        mockWalletDeduction(false, true);
        mockRollback(false);
        MockHttpClient.forUrl(billSaveUrl).callCount(1).response(200, getResponseWithFailCode(1275));
        MockHttpClient.forUrl(newTokenUrl).response(200, getNewTokenAPIResponse());
        MockHttpClient.forUrl(billSaveUrl).callCount(2).response(200, getResponseWithFailCode(1275));

        //Billpay status rollback
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.ROLLBACK));

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1275 #New Token 1200 #Second Save Bill 1275");
    }

    @Test
    public void case18() throws Exception {
        logger.debug("Test Case Started: #Associated #Wallet Deducted #Save Bill 1275 #New Token other than 1200, 1275, 1273");

        mockWalletDeduction(false, true);
        mockRollback(false);
        MockHttpClient.forUrl(billSaveUrl).response(200, getResponseWithFailCode(1275));
        MockHttpClient.forUrl(newTokenUrl).response(200, getResponseWithFailCode(1290));

        //Rollback
        callPayment(false).andExpect(jsonPath("response.status").value(BillPayStatus.ROLLBACK));

        logger.debug("Test Case Ended: #Associated #Wallet Deducted #Save Bill 1275 #New Token other than 1200, 1275, 1273");
    }

    @AfterEach
    public void clearDbRecords() {
        queryExecutorRepository.executeUpdate("delete from " + mfsSchema + ".mtx_transaction_header");
        prepaidBillTokenRepository.deleteAll(); //Though no entry added by this class, it is called for safety
        disputeTransactionRepository.deleteAll();
        billPayServiceStatusRepository.deleteAll();
    }

    @AfterAll
    public void clearAssociation() {
        queryExecutorRepository.executeUpdate("delete from " + mfsSchema + ".MNY_UTILITY_SUBSCRIBER");
    }
}