package com.grameenphone.wipro.fmfs.mfs_processor.test.breb;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.Category;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.Company;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.RebAuthenticationApiDetail;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.CategoryRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.CompanyRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.breb.AuthenticationApiRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.breb.RebPostpaidService;
import com.grameenphone.wipro.fmfs.mfs_processor.test.utility.BillTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.MockMvc;

public class RebBillTestBase extends BillTestSupport {
    protected static final String TEST_CONSUMER_ID = "1069022351047";
    protected static final String TEST_BILL_NO = "10690223532450321";
    protected static final Logger logger = LoggerFactory.getLogger(RebBillTestBase.class);

    @Autowired
    MockMvc mockMvc;

    @Value("${reb_postpaid_bill_query_url}")
    String billQueryUrl;

    @Value("${reb_postpaid_new_token_url}")
    String newTokenUrl;

    @Value("${reb_postpaid_authentication_url}")
    String authenticationUrl;

    @Autowired
    CompanyRepository companyRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    AuthenticationApiRepository authenticationApiRepository;

    @BeforeAll
    protected void insertAssociation() {
        billAmount = 10;
        try {
            clearAssociation();
            insertAssociation(RebPostpaidService.COMPANY_CODE, TEST_CONSUMER_ID);
            Company company = companyRepository.findCompanyByCompanyCode(RebPostpaidService.COMPANY_CODE);
            if(company == null) {
                Category category = categoryRepository.findCategoryByCategoryCode("ELEC POST");
                if(category == null) {
                    category = new Category();
                    category.categoryCode = "ELEC POST";
                    category.categoryName = "Electricity Postpaid";
                    categoryRepository.save(category);
                }
                company = new Company();
                company.category = category;
                company.companyCode = RebPostpaidService.COMPANY_CODE;
                company.companyName = "Test BREB";
                company.status = "Active";
                companyRepository.save(company);
            }
        } catch (Throwable j) {
            logger.error("ERROR!!!", j);
        } //Not to prevent test execution for any error
    }

    @BeforeAll
    protected void insertInitialToken() {
        try {
            authenticationApiRepository.deleteAll();
            RebAuthenticationApiDetail authenticationApiDetail = new RebAuthenticationApiDetail();
            authenticationApiDetail.access_token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVU0VSX0lEIjoiMDEzNzIzMDEtR3JhbWVlblBob25lIiwibmJmIjoxNjMzMDA5NDQ3LCJleHAiOjE2MzMwMDk3NDcsImlhdCI6MTYzMzAwOTQ0NywiaXNzIjoie1wiSVNTVUVSX05BTUVcIjpcIkJhbmdsYWRlc2ggUnVyYWwgRWxlY3RyaWZpY2F0aW9uIEJvYXJkXCIsXCJJU1NVRVJfVFlQRVwiOlwiRWxlY3RyaWNpdHkgRGlzdHJpYnV0b3JcIixcIklTU1VFUl9VUkxcIjpcImh0dHA6Ly9yZWIuZ292LmJkL1wifSIsImF1ZCI6Ilt7XCJBVURJRU5DRV9OQU1FXCI6XCJEYXRjaC1CYW5nbGEgQmFuayBMaW1pdGVkXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJCYW5rXCIsXCJBVURJRU5DRV9VUkxcIjpcImh0dHBzOi8vd3d3LmR1dGNoYmFuZ2xhYmFuay5jb20vXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcIkdyYW1lZW5waG9uZVwiLFwiQVVESUVOQ0VfVFlQRVwiOlwiTUZTXCIsXCJBVURJRU5DRV9VUkxcIjpcImh0dHBzOi8vd3d3LmdyYW1lZW5waG9uZS5jb20vXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcImJLYXNoXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJNRlNcIixcIkFVRElFTkNFX1VSTFwiOlwiaHR0cHM6Ly93d3cuYmthc2guY29tL1wifSx7XCJBVURJRU5DRV9OQU1FXCI6XCJSdXBhbGkgQmFuayBMaW1pdGVkLVN1cmVDYXNoXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJNRlNcIixcIkFVRElFTkNFX1VSTFwiOlwiaHR0cHM6Ly93d3cucnVwYWxpYmFuay5vcmcvXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcIlJvYmkgQXhpYXRhXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJNRlNcIixcIkFVRElFTkNFX1VSTFwiOlwiaHR0cHM6Ly93d3cucm9iaS5jb20uYmQvZW5cIn0se1wiQVVESUVOQ0VfTkFNRVwiOlwiVW5pdGVkIENvbW1lcmNpYWwgQmFuayBMaW1pdGVkXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJCYW5rXCIsXCJBVURJRU5DRV9VUkxcIjpcImh0dHBzOi8vd3d3LnVjYi5jb20uYmQvXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcIk1hcmtlbnRhaWwgQmFuayBMaW5taXRlZFwiLFwiQVVESUVOQ0VfVFlQRVwiOlwiQmFua1wiLFwiQVVESUVOQ0VfVVJMXCI6XCJodHRwczovL3d3dy5teWNhc2htYmwuY29tL1wifV0ifQ.51_S58eAj-blrJ4T5p3krCFSjsEf7T1Rq_PNHXsix9I";
            authenticationApiDetail.refresh_token = "HqrxrqqzaGK9rVTZXPTSP3d02ID8hwqIRjTb0CLWkU/QLj6seHGKYUBCiieOZNcm4mud99xgh/G82mqTnkILYA==";
            authenticationApiRepository.save(authenticationApiDetail);
        } catch (Throwable j) {
            logger.error("ERROR!!!", j);
        } //Not to prevent test execution for any error
    }

    protected String getDueBillResponse() {
        return "{\"DATA\": {\n" +
                "    \"BILL_NO\": \"" + TEST_BILL_NO + "\",\n" +
                "    \"BOOK_NO\": \"235\",\n" +
                "    \"SMS_AC_NO\": \"" + TEST_CONSUMER_ID + "\",\n" +
                "    \"BILL_MONTH\": \"03\",\n" +
                "    \"BILL_YEAR\": \"2021\",\n" +
                "    \"ISSUE_DATE\": \"07/03/2021\",\n" +
                "    \"DUE_DATE\": \"30/04/2021\",\n" +
                "    \"DUE_AMOUNT\": 462,\n" +
                "    \"DUE_TYPE\": \"DUE_WITHOUT_LPC\",\n" +
                "    \"LPC_DATE\": \"15/05/2021\",\n" +
                "    \"PAID_STATUS\": \"unpaid\",\n" +
                "    \"PBS_CODE\": \"069\",\n" +
                "    \"PBS_NAME_B\": \"গাজীপুর পবিস-১\",\n" +
                "    \"PBS_NAME_E\": \"Gazipur PBS-1\"\n" +
                "  },\n" +
                "  \"RESPONSE\": {\n" +
                "    \"RESPONSE_MSG\": \"DATA TRANSACTION SUCCESSFULL\",\n" +
                "    \"RESPONSE_CODE\": 1200\n" +
                "  }\n" +
                "}";
    }

    protected String getResponseWithFailCode(int code) {
        return "{\"DATA\": null,\n" +
                "  \"RESPONSE\": {\n" +
                "    \"RESPONSE_MSG\": \"XXXXXXXXXXXXXXXXXXXXXXXX\",\n" +
                "    \"RESPONSE_CODE\": " + code + "\n" +
                "  }\n" +
                "}";
    }

    protected String getSaveAndAcknowledgementResponse() {
        return "{\"DATA\": null,\n" +
                "  \"RESPONSE\": {\n" +
                "    \"RESPONSE_MSG\": \"XXXXXXXXXXXXXXXXXXXXXXXX\",\n" +
                "    \"RESPONSE_CODE\": 1200\n" +
                "  }\n" +
                "}";
    }

    String renewedAccessTokenFromNewToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVU0VSX0lEIjoiMDEzNzIzMDEtR3JhbWVlblBob25lIiwibmJmIjoxNjMyNzQ0MzkzLCJleHAiOjE2MzI3NDQ2OTMsImlhdCI6MTYzMjc0NDM5MywiaXNzIjoie1wiSVNTVUVSX05BTUVcIjpcIkJhbmdsYWRlc2ggUnVyYWwgRWxlY3RyaWZpY2F0aW9uIEJvYXJkXCIsXCJJU1NVRVJfVFlQRVwiOlwiRWxlY3RyaWNpdHkgRGlzdHJpYnV0b3JcIixcIklTU1VFUl9VUkxcIjpcImh0dHA6Ly9yZWIuZ292LmJkL1wifSIsImF1ZCI6Ilt7XCJBVURJRU5DRV9OQU1FXCI6XCJEYXRjaC1CYW5nbGEgQmFuayBMaW1pdGVkXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJCYW5rXCIsXCJBVURJRU5DRV9VUkxcIjpcImh0dHBzOi8vd3d3LmR1dGNoYmFuZ2xhYmFuay5jb20vXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcIkdyYW1lZW5waG9uZVwiLFwiQVVESUVOQ0VfVFlQRVwiOlwiTUZTXCIsXCJBVURJRU5DRV9VUkxcIjpcImh0dHBzOi8vd3d3LmdyYW1lZW5waG9uZS5jb20vXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcImJLYXNoXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJNRlNcIixcIkFVRElFTkNFX1VSTFwiOlwiaHR0cHM6Ly93d3cuYmthc2guY29tL1wifSx7XCJBVURJRU5DRV9OQU1FXCI6XCJSdXBhbGkgQmFuayBMaW1pdGVkLVN1cmVDYXNoXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJNRlNcIixcIkFVRElFTkNFX1VSTFwiOlwiaHR0cHM6Ly93d3cucnVwYWxpYmFuay5vcmcvXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcIlJvYmkgQXhpYXRhXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJNRlNcIixcIkFVRElFTkNFX1VSTFwiOlwiaHR0cHM6Ly93d3cucm9iaS5jb20uYmQvZW5cIn0se1wiQVVESUVOQ0VfTkFNRVwiOlwiVW5pdGVkIENvbW1lcmNpYWwgQmFuayBMaW1pdGVkXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJCYW5rXCIsXCJBVURJRU5DRV9VUkxcIjpcImh0dHBzOi8vd3d3LnVjYi5jb20uYmQvXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcIk1hcmtlbnRhaWwgQmFuayBMaW5taXRlZFwiLFwiQVVESUVOQ0VfVFlQRVwiOlwiQmFua1wiLFwiQVVESUVOQ0VfVVJMXCI6XCJodHRwczovL3d3dy5teWNhc2htYmwuY29tL1wifV0ifQ.qxtEFUflpN_VEAw3NtZ2-ctUqYIx7XgeYZgQY5wshJA";

    protected String getNewTokenAPIResponse() {
        return "{\n" +
                "  \"DATA\": {\n" +
                "    \"TOKEN\": {\n" +
                "      \"REFRESH_TOKEN\": \"f3QqDWoR0kd7h2pbNzkd8TeWX3jzrsfQ24YTl9VInW5kO4LvQbi5yutUX11ji4Y95HXQ+TWcYYBH1MWtEmQpCA==\",\n" +
                "      \"ACCESS_TOKEN\": \"" + renewedAccessTokenFromNewToken + "\",\n" +
                "      \"TIMESTAMP\": \"27/09/2021 18:06:33.098000000 +06:00\",\n" +
                "      \"NO_OF_ATTEMPT\": 2\n" +
                "    }\n" +
                "  },\n" +
                "  \"RESPONSE\": {\n" +
                "    \"RESPONSE_MSG\": \"DATA TRANSACTION SUCCESSFULL\",\n" +
                "    \"RESPONSE_CODE\": 1200\n" +
                "  }\n" +
                "}";
    }

    protected String getAuthenticationAPIResponse() {
        return "{\n" +
                "  \"DATA\": {\n" +
                "    \"TOKEN\": {\n" +
                "      \"REFRESH_TOKEN\": \"HqrxrqqzaGK9rVTZXPTSP3d02ID8hwqIRjTb0CLWkU/QLj6seHGKYUBCiieOZNcm4mud99xgh/G82mqTnkILYA==\",\n" +
                "      \"ACCESS_TOKEN\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJVU0VSX0lEIjoiMDEzNzIzMDEtR3JhbWVlblBob25lIiwibmJmIjoxNjMzMDA5NDQ3LCJleHAiOjE2MzMwMDk3NDcsImlhdCI6MTYzMzAwOTQ0NywiaXNzIjoie1wiSVNTVUVSX05BTUVcIjpcIkJhbmdsYWRlc2ggUnVyYWwgRWxlY3RyaWZpY2F0aW9uIEJvYXJkXCIsXCJJU1NVRVJfVFlQRVwiOlwiRWxlY3RyaWNpdHkgRGlzdHJpYnV0b3JcIixcIklTU1VFUl9VUkxcIjpcImh0dHA6Ly9yZWIuZ292LmJkL1wifSIsImF1ZCI6Ilt7XCJBVURJRU5DRV9OQU1FXCI6XCJEYXRjaC1CYW5nbGEgQmFuayBMaW1pdGVkXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJCYW5rXCIsXCJBVURJRU5DRV9VUkxcIjpcImh0dHBzOi8vd3d3LmR1dGNoYmFuZ2xhYmFuay5jb20vXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcIkdyYW1lZW5waG9uZVwiLFwiQVVESUVOQ0VfVFlQRVwiOlwiTUZTXCIsXCJBVURJRU5DRV9VUkxcIjpcImh0dHBzOi8vd3d3LmdyYW1lZW5waG9uZS5jb20vXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcImJLYXNoXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJNRlNcIixcIkFVRElFTkNFX1VSTFwiOlwiaHR0cHM6Ly93d3cuYmthc2guY29tL1wifSx7XCJBVURJRU5DRV9OQU1FXCI6XCJSdXBhbGkgQmFuayBMaW1pdGVkLVN1cmVDYXNoXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJNRlNcIixcIkFVRElFTkNFX1VSTFwiOlwiaHR0cHM6Ly93d3cucnVwYWxpYmFuay5vcmcvXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcIlJvYmkgQXhpYXRhXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJNRlNcIixcIkFVRElFTkNFX1VSTFwiOlwiaHR0cHM6Ly93d3cucm9iaS5jb20uYmQvZW5cIn0se1wiQVVESUVOQ0VfTkFNRVwiOlwiVW5pdGVkIENvbW1lcmNpYWwgQmFuayBMaW1pdGVkXCIsXCJBVURJRU5DRV9UWVBFXCI6XCJCYW5rXCIsXCJBVURJRU5DRV9VUkxcIjpcImh0dHBzOi8vd3d3LnVjYi5jb20uYmQvXCJ9LHtcIkFVRElFTkNFX05BTUVcIjpcIk1hcmtlbnRhaWwgQmFuayBMaW5taXRlZFwiLFwiQVVESUVOQ0VfVFlQRVwiOlwiQmFua1wiLFwiQVVESUVOQ0VfVVJMXCI6XCJodHRwczovL3d3dy5teWNhc2htYmwuY29tL1wifV0ifQ.51_S58eAj-blrJ4T5p3krCFSjsEf7T1Rq_PNHXsix9I\",\n" +
                "      \"TIMESTAMP\": \"09/30/2021 19:44:07.597 Asia/Dhaka\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"RESPONSE\": {\n" +
                "    \"RESPONSE_MSG\": \"DATA TRANSACTION SUCCESSFULL\",\n" +
                "    \"RESPONSE_CODE\": 1200\n" +
                "  }\n" +
                "}";
    }

    @AfterAll
    protected void clearAssociation() {
        billAmount = 10;
        try {
            super.clearAssociation();
        } finally {
        }
    }
}