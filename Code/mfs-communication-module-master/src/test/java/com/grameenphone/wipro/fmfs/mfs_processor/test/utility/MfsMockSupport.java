package com.grameenphone.wipro.fmfs.mfs_processor.test.utility;

import com.grameenphone.wipro.fmfs.mfs_communicator.repository.mfs.QueryExecutorRepository;
import com.grameenphone.wipro.utility.KV;
import com.grameenphone.wipro.utility.common.MockHttpClient;
import com.grameenphone.wipro.utility.common.MockHttpClient.NotifyCondition;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MfsMockSupport {
    @Value("${mfs.datasource.default.schema}")
    protected String mfsSchema;

    @Value("${mfs.api.path}")
    protected String mfsApiPath;

    @Value("${test.retailer.pin}")
    protected String retailerWalletPin;
    
    @Value("${test.subscriber.msisdn}")
    protected String subscriberMsisdn;
    
    @Value("${test.subscriber.pin}")
    protected String subscriberWalletPin;

    @Value("${test.retailer.msisdn}")
    protected String retailerMsisdn;

    protected String mockedMFSTxnid;
    
    @Autowired
    protected QueryExecutorRepository queryExecutorRepository;

    protected String companyCode;
    protected double billAmount;

    protected final static  String mfsErrorMessage = "Intentional Failure From Environment";

    protected String mfsGenericSuccessResponse = "<!DOCTYPE COMMAND PUBLIC \"-//Ocam//DTD XML Command 1.0//EN\" \"xml/command.dtd\">" +
            "<COMMAND>" +
            "<TYPE>BPREGRESP</TYPE>" +
            "<MSISDN></MSISDN>" +
            "<TXNSTATUS>200</TXNSTATUS>" +
            "<TXNID>XX121212.4523.A45392</TXNID>" +
            "<INTERVAL></INTERVAL>" +
            "<MESSAGE></MESSAGE>" +
            "</COMMAND>";

    protected void mockAssociation(boolean isSubscriber) {
        try {
            MockHttpClient.forUrl(mfsApiPath + "&REQUEST_GATEWAY_TYPE=USSD").isPost().content(isSubscriber ? "BPREGREQ" : "RBPREGAREQ", false).response(200, mfsGenericSuccessResponse);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Association URL could not be mocked");
        }
    }

    protected NotifyCondition notifyAssociation(boolean isSubscriber) {
        try {
            return MockHttpClient.notifyFor(mfsApiPath + "&REQUEST_GATEWAY_TYPE=USSD").isPost().content(isSubscriber ? "BPREGREQ" : "RBPREGAREQ", false);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Association URL could not be mocked");
        }
    }

    protected String getWalletDeductionSuccessResponse(boolean isSubscriber) {
        String a = new SimpleDateFormat("yyMMdd").format(new Date());
        String b = new SimpleDateFormat("HHmm").format(new Date());
        String c = RandomStringUtils.randomNumeric(5);
        mockedMFSTxnid = "BP" + a + "." + b + ".A" + c;
        return "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE COMMAND PUBLIC \"-//Ocam//DTD XML Command 1.0//EN\" \"xml/command.dtd\">\n" +
                "<COMMAND>\n" +
                "\t<TYPE>" + (isSubscriber ? "CPMBRESP" : "RPMBRESP") + "</TYPE>\n" +
                "\t\t<TXNID>" + mockedMFSTxnid + "</TXNID>\n" +
                "\t\t<TXNSTATUS>200</TXNSTATUS>\n" +
                "\t\t<BILLCCODE>" + companyCode + "</BILLCCODE>\n" +
                "\t\t<BILLNO>" + RandomStringUtils.randomNumeric(16) + "</BILLNO>\n" +
                "\t\t<BDUDATE>" + new SimpleDateFormat("dd/MM/yy").format(new Date()) + "</BDUDATE>\n" +
                "\t\t<AMOUNT>" + billAmount + "</AMOUNT>\n" +
                "\t\t<MESSAGE>Your due bill is paid successfully. You will get payment confirmation SMS within short time.</MESSAGE>\n" +
                "\t\t<TRID>" + (isSubscriber ? subscriberMsisdn : retailerMsisdn) + "20" + a + b + "A" + c + "</TRID>\n" +
                "\t</COMMAND>";
    }

    protected String getWalletDeductionFailResponse(boolean isSubscriber) {
        return "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE COMMAND PUBLIC \"-//Ocam//DTD XML Command 1.0//EN\" \"xml/command.dtd\">\n" +
                "<COMMAND>\n" +
                "\t<TYPE>" + (isSubscriber ? "CPMBRESP" : "RPMBRESP") + "</TYPE>\n" +
                "\t\t<TXNID></TXNID>\n" +
                "\t\t<TXNSTATUS>45297</TXNSTATUS>\n" +
                "\t\t<BILLCCODE>" + companyCode + "</BILLCCODE>\n" +
                "\t\t<BILLNO></BILLNO>\n" +
                "\t\t<BDUDATE>" + new SimpleDateFormat("dd/MM/yy").format(new Date()) + "</BDUDATE>\n" +
                "\t\t<AMOUNT>" + billAmount + "</AMOUNT>\n" +
                "\t\t<MESSAGE>" + mfsErrorMessage + "</MESSAGE>\n" +
                "\t\t<TRID></TRID>\n" +
                "\t</COMMAND>";
    }

    protected void mockWalletDeduction(boolean isSubscriber, boolean shouldMfsSuccess) {
        try {
            MockHttpClient.forUrl(mfsApiPath + "&REQUEST_GATEWAY_TYPE=USSD").isPost().content(isSubscriber ? "CPMBREQ" : "RPMBREQ", false).response(200, shouldMfsSuccess ? getWalletDeductionSuccessResponse(isSubscriber) : getWalletDeductionFailResponse(isSubscriber));
            if(shouldMfsSuccess) {
                insertServiceChargeEntry();
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException("Association URL could not be mocked");
        }
    }

    protected void mockRollback(boolean isSubscriber) {
        try {
            MockHttpClient.forUrl(mfsApiPath + "&REQUEST_GATEWAY_TYPE=USSD").isPost().content("REQTRCORIN", false).response(200, mfsGenericSuccessResponse);
            MockHttpClient.forUrl(mfsApiPath + "&REQUEST_GATEWAY_TYPE=USSD").isPost().content("REQTRCORCF", false).response(200, mfsGenericSuccessResponse);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Rollback URL could not be mocked");
        }
    }

    private void insertServiceChargeEntry() {
        queryExecutorRepository.executeUpdate("insert into mtx_transaction_header (transfer_id, CREATED_BY, created_on, modified_by, modified_on, transfer_value, total_service_charge, transfer_status) values (:tid, 'TEST', :date, 'TEST', :date, :amount, 500, 'TS')", new KV<>("tid", mockedMFSTxnid), new KV<>("date", new Date()), new KV<>("amount", billAmount * 100 + 500));
    }
    
    protected void insertAssociation(String companyCode, String account) {
        queryExecutorRepository.executeUpdate("Insert into " + mfsSchema + ".MNY_UTILITY_SUBSCRIBER (COMPANY_CODE,ACCOUNT_NUMBER,USER_NUMBER,MSISDN,STATUS_ID,CREATED_BY,CREATED_ON,MODIFIED_BY,MODIFIED_ON,BATCH_ID,ATTR_NAME1,ATTR_VALUE1,ATTR_NAME5,ATTR_VALUE5,PROVIDER_ID) values ('" + companyCode + "','" + account + "',1,'" + subscriberMsisdn + "','Y','SU0001',to_date('15-AUG-08','DD-MON-RR'),'SU0001',to_date('15-AUG-08','DD-MON-RR'),'BA080815.0625.000001','AccountNumber','" + account + "','ELECTRICITY','Electricity',101)");
    }

    protected void clearAssociation() {
        queryExecutorRepository.executeUpdate("truncate table " + mfsSchema + ".MNY_UTILITY_SUBSCRIBER");
    }
}