package com.grameenphone.wipro.fmfs.mfs_communicator.service.nescoPrepaid;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid.NescoTokenRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid.NescoTokenResponse;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.HttpClient.HttpRequestSnapshot;
import com.grameenphone.wipro.utility.marshal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

@Service
public class NescoPrepaidAccesTokenService {
    protected static final Logger logger = LoggerFactory.getLogger(NescoPrepaidAccesTokenService.class);
    public static NescoTokenRequest tokenRequest;
    private static volatile NescoTokenResponse lastTokenReceived;
    
    @Value("${nesco_prepaid_request_timeout}")
    int timeout;
    @Value("${nesco_prepaid_app_name}")
    String appName;
    @Value("${nesco_prepaid_app_password}")
    String appPassword;
    @Value("${nesco_prepaid_post_auth_code}")
    String authCode;
    @Value("${nesco_prepaid_proxy_required}")
    Boolean isProxyRequired;
    @Value("${nesco_prepaid_login_url}")
    String loginUrl;

    @PostConstruct
    private void initialize() {
        tokenRequest = new NescoTokenRequest();
        tokenRequest.app_name = appName;
        tokenRequest.app_password = appPassword;
        tokenRequest.postAuthCode = authCode;
    }

    public String getToken() throws IOException {
        synchronized (NescoPrepaidAccesTokenService.class) {
            if (lastTokenReceived != null) {
                if (lastTokenReceived.expirationTimeInMilli - 120000 >= new Date().getTime()) { //2 min deducted for time safety
                    logger.debug("Reusing stored token: (" + lastTokenReceived.expirationTimeInMilli + "): " + lastTokenReceived.access_token);
                    return lastTokenReceived.access_token;
                }
                lastTokenReceived = null;
            }
            HttpClient client = new HttpClient(timeout);
            if (isProxyRequired) {
                client.setDefaultProxy();
            }
            client.setPayloadLoggerInterceptor((HttpRequestSnapshot httpRequestSnapshot) -> httpRequestSnapshot.body.replaceAll("\"app_password\":\"[^\"]*\"", "\"app_password\":\"****\"").replaceAll("\"postAuthCode\":\"[^\"]*\"", "\"postAuthCode\":\"********-****-****-****-************\""));
            lastTokenReceived = client.postForEntity(loginUrl, Json.toJson(tokenRequest), new HashMap<>() {{
                put("Content-Type", "application/json");
            }}, NescoTokenResponse.class);
            lastTokenReceived.expirationTimeInMilli = new Date().getTime() + Integer.parseInt(lastTokenReceived.expires_in);
            return lastTokenReceived.access_token;
        }
    }
}