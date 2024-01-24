package com.grameenphone.wipro.fmfs.mfs_processor.test.breb;

import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import com.grameenphone.wipro.fmfs.mfs_communicator.config.FlexMFSDbConfig;
import com.grameenphone.wipro.fmfs.mfs_communicator.config.MFSDbConfig;
import com.grameenphone.wipro.fmfs.mfs_communicator.config.MVCConfig;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.RebBillDetail;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.breb.BillDetailRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.breb.RebErrors;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.breb.RebPostpaidService;
import com.grameenphone.wipro.fmfs.mfs_processor.test.config.TestContextLoader;
import com.grameenphone.wipro.fmfs.mfs_processor.test.config.TestExtension;
import com.grameenphone.wipro.utility.common.MockHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ExtendWith(TestExtension.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {Application.class, FlexMFSDbConfig.class, MFSDbConfig.class, MVCConfig.class}, loader = TestContextLoader.class)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestExecutionListeners({DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class DueBillTest extends RebBillTestBase {
    @Autowired
    BillDetailRepository billDetailRepository;

    @Test
    public void gotProperResponseWithValidToken() throws Exception {
        logger.debug("Test Case Started: SUCCESS RESPONSE WITH BILL AND NO PROGRESS PAYMENT AND NO VALID AUTH IN DB");

        insertInitialToken();

        String dueBillResponse = getDueBillResponse();
        String tokenResponse = getNewTokenAPIResponse();
        String authResponse = getAuthenticationAPIResponse();
        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).response(200, dueBillResponse, "UTF-8");
        MockHttpClient.forUrl(newTokenUrl).response(200, tokenResponse, "UTF-8");
        MockHttpClient.forUrl(authenticationUrl).response(200, authResponse, "UTF-8");
        MockHttpClient.NotifyCondition tokenNotifier = MockHttpClient.notifyFor(newTokenUrl);
        MockHttpClient.NotifyCondition authNotifier = MockHttpClient.notifyFor(authenticationUrl);

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("response.bills").isNotEmpty()).andExpect(jsonPath("response.bills[0].amount").value(462)).andExpect(jsonPath("response.bills[0].detail.PBS_NAME_E").value("Gazipur PBS-1"));

        RebBillDetail billDetail = billDetailRepository.findBySmsAccountNumber(TEST_CONSUMER_ID);
        Assertions.assertNotNull(billDetail);
        Assertions.assertEquals(false, tokenNotifier.isCalled());
        Assertions.assertEquals(false, authNotifier.isCalled());

        assertThatJson(billDetail.brebResponse.toLowerCase()).isEqualTo(dueBillResponse.toLowerCase());

        logger.debug("Test Case Ended: SUCCESS RESPONSE WITH BILL AND NO PROGRESS PAYMENT");
    }

    @Test
    public void gotProperResponseWithInvalidInitialTokenAndValidRefreshToken() throws Exception {
        logger.debug("Test Case Started: SUCCESS RESPONSE WITH BILL AND NO PROGRESS PAYMENT AND INVALID ACCESS BUT VALID REFRESH TOKEN");

        insertInitialToken();

        String invalidTokenResponse = getResponseWithFailCode(1271);
        String dueBillResponse = getDueBillResponse();
        String tokenResponse = getNewTokenAPIResponse();
        String authResponse = getAuthenticationAPIResponse();
        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).callCount(1).response(200, invalidTokenResponse, "UTF-8");
        MockHttpClient.forUrl(billQueryUrl).callCount(2).response(200, dueBillResponse, "UTF-8");
        MockHttpClient.forUrl(newTokenUrl).response(200, tokenResponse, "UTF-8");
        MockHttpClient.forUrl(authenticationUrl).response(200, authResponse, "UTF-8");
        MockHttpClient.NotifyCondition tokenNotifier = MockHttpClient.notifyFor(newTokenUrl);
        MockHttpClient.NotifyCondition authNotifier = MockHttpClient.notifyFor(authenticationUrl);

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("response.bills").isNotEmpty()).andExpect(jsonPath("response.bills[0].amount").value(462)).andExpect(jsonPath("response.bills[0].detail.PBS_NAME_E").value("Gazipur PBS-1"));

        RebBillDetail billDetail = billDetailRepository.findBySmsAccountNumber(TEST_CONSUMER_ID);
        Assertions.assertNotNull(billDetail);
        Assertions.assertEquals(true, tokenNotifier.isCalled());
        Assertions.assertEquals(false, authNotifier.isCalled());

        assertThatJson(billDetail.brebResponse.toLowerCase()).isEqualTo(dueBillResponse.toLowerCase());

        logger.debug("Test Case Ended: SUCCESS RESPONSE WITH BILL AND NO PROGRESS PAYMENT");
    }

    @Test
    public void gotProperResponseWithInvalidInitialTokenAndInvalidRefreshToken() throws Exception {
        logger.debug("Test Case Started: SUCCESS RESPONSE WITH BILL AND NO PROGRESS PAYMENT AND INVALID ACCESS BUT VALID REFRESH TOKEN");

        insertInitialToken();

        String invalidTokenResponse = getResponseWithFailCode(1271);
        String dueBillResponse = getDueBillResponse();
        String tokenResponse = getResponseWithFailCode(1273);
        String authResponse = getAuthenticationAPIResponse();
        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).callCount(1).response(200, invalidTokenResponse, "UTF-8");
        MockHttpClient.forUrl(billQueryUrl).callCount(2).response(200, dueBillResponse, "UTF-8");
        MockHttpClient.forUrl(newTokenUrl).response(200, tokenResponse, "UTF-8");
        MockHttpClient.forUrl(authenticationUrl).response(200, authResponse, "UTF-8");
        MockHttpClient.NotifyCondition tokenNotifier = MockHttpClient.notifyFor(newTokenUrl);
        MockHttpClient.NotifyCondition authNotifier = MockHttpClient.notifyFor(authenticationUrl);

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("response.bills").isNotEmpty()).andExpect(jsonPath("response.bills[0].amount").value(462)).andExpect(jsonPath("response.bills[0].detail.PBS_NAME_E").value("Gazipur PBS-1"));

        RebBillDetail billDetail = billDetailRepository.findBySmsAccountNumber(TEST_CONSUMER_ID);
        Assertions.assertNotNull(billDetail);
        Assertions.assertEquals(true, tokenNotifier.isCalled());
        Assertions.assertEquals(true, authNotifier.isCalled());

        assertThatJson(billDetail.brebResponse.toLowerCase()).isEqualTo(dueBillResponse.toLowerCase());

        logger.debug("Test Case Ended: SUCCESS RESPONSE WITH BILL AND NO PROGRESS PAYMENT");
    }

    @Test
    public void gotSecondCallForSuccessDueBill() throws Exception {
        logger.debug("Test Case Started: SUCCESS RESPONSE WITH BILL WITH PROGRESS PAYMENT");

        String rebResponse = getResponseWithFailCode(1274);
        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).response(200, rebResponse, "UTF-8");
        insertBillCache();

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("response.bills").isNotEmpty()).andExpect(jsonPath("response.bills[0].amount").value(462)).andExpect(jsonPath("response.bills[0].detail.PBS_NAME_E").value("Gazipur PBS-1"));

        logger.debug("Test Case Ended: SUCCESS RESPONSE WITH BILL WITH PROGRESS PAYMENT");
    }

    @Test
    public void gotProgressDueBillFromThirdParty() throws Exception {
        logger.debug("Test Case Started: SUCCESS RESPONSE WITH BILL WITH THIRD PARTY PROGRESS PAYMENT");

        String rebResponse = getResponseWithFailCode(1274);
        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).response(200, rebResponse, "UTF-8");

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("status").value(422)).andExpect(jsonPath("message").value(RebErrors._1274));

        logger.debug("Test Case Ended: SUCCESS RESPONSE WITH BILL WITH THIRD PARTY PROGRESS PAYMENT");
    }

    @Test
    public void gotInvalidAccountNumber() throws Exception {
        logger.debug("Test Case Started: INVALID ACCOUNT NUMBER");

        String rebResponse = getResponseWithFailCode(1290);
        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).response(200, rebResponse, "UTF-8");

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("status").value(422)).andExpect(jsonPath("message").value(RebErrors._1290));

        logger.debug("Test Case Ended: INVALID ACCOUNT NUMBER");
    }

    @Test
    public void billAcknowledgementNotCalled() throws Exception {
        logger.debug("Test Case Started: BILL ACKNOWLEDGEMENT NOT CALLED");

        String rebResponse = getResponseWithFailCode(1261);
        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).response(200, rebResponse, "UTF-8");

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("status").value(422)).andExpect(jsonPath("message").value(RebErrors._1261));

        logger.debug("Test Case Ended: BILL ACKNOWLEDGEMENT NOT CALLED");
    }

    @Test
    public void noDueBill() throws Exception {
        logger.debug("Test Case Started: NO DUE BILL");

        String rebResponse = getResponseWithFailCode(1278);
        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).response(200, rebResponse, "UTF-8");

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("response.bills").isEmpty());

        logger.debug("Test Case Ended: NO DUE BILL");
    }

    @Test
    public void garbageResponseCode() throws Exception {
        logger.debug("Test Case Started: SUCCESS RESPONSE WITH BILL WITH PROGRESS PAYMENT");

        String rebResponse = getResponseWithFailCode(1250);
        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).response(200, rebResponse, "UTF-8");

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("status").value(520)).andExpect(jsonPath("message").value(RebErrors.BILL_FETCH));

        logger.debug("Test Case Ended: SUCCESS RESPONSE WITH BILL WITH PROGRESS PAYMENT");
    }

    @Test
    public void garbageResponse() throws Exception {
        logger.debug("Test Case Started: SUCCESS RESPONSE WITH BILL WITH PROGRESS PAYMENT");

        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).response(200, "GARBAGE RESPONSE", "UTF-8");

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("status").value(520)).andExpect(jsonPath("message").value(RebErrors.BILL_FETCH));

        logger.debug("Test Case Ended: SUCCESS RESPONSE WITH BILL WITH PROGRESS PAYMENT");
    }

    @Test
    public void dueBillCallTimedOut() throws Exception {
        logger.debug("Test Case Started: SUCCESS RESPONSE WITH BILL WITH PROGRESS PAYMENT");

        MockHttpClient.reset();
        MockHttpClient.forUrl(billQueryUrl).timeout();

        mockMvc.perform(post("/bill/dues").contentType(MediaType.APPLICATION_JSON).content(getDueBillRequest(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID))).andExpect(jsonPath("status").value(520)).andExpect(jsonPath("message").value(RebErrors.BILL_FETCH));

        logger.debug("Test Case Ended: SUCCESS RESPONSE WITH BILL WITH PROGRESS PAYMENT");
    }

    @AfterEach
    public void clearBillDetailTable() {
        billDetailRepository.deleteAll();
    }

    private void insertBillCache() {
        RebBillDetail billDetail = new RebBillDetail();
        billDetail.brebResponse = getDueBillResponse();
        billDetail.smsAccountNumber = TEST_CONSUMER_ID;
        billDetailRepository.save(billDetail);
    }
}