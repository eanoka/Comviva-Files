package com.grameenphone.wipro.fmfs.mfs_communicator.service.descopost;

import java.util.Date;
import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.HttpClient.HttpRequestSnapshot;
import com.grameenphone.wipro.utility.marshal.Json;

@Service
public class DescoPostpaidAccesTokenService {
    protected static final Logger logger = LoggerFactory.getLogger(DescoPostpaidAccesTokenService.class);
    public static DescoPostpaidTokenRequest tokenRequest;
    private static volatile DescoPostpaidTokenResponse lastTokenReceived;

    @Value("${desco_postpaid_username}")
    String username;
    @Value("${desco_postpaid_password}")
    String password;
    @Value("${desco_postpaid_request_timeout}")
    int timeout;
    @Value("${desco_postpaid_proxy_required}")
    Boolean isProxyRequired;
    @Value("${desco_postpaid_login_url}")
    String loginUrl;
    @Value("${desco_postpaid_token_expiration_duration_in_millisecond}")
    long expireDuration;
    private boolean isTokenExpired;

    @PostConstruct
    private void initialize() {
        tokenRequest = new DescoPostpaidTokenRequest();
        tokenRequest.username = username;
        tokenRequest.password = password;
    }

    public String getToken() throws Exception {
        synchronized (DescoPostpaidAccesTokenService.class) {
            if (lastTokenReceived != null && !isTokenExpired) {
                if (lastTokenReceived.getExpirationTimeInMilli() - 180000 >= new Date().getTime()) { //3 mins deducted for time safety
                    return lastTokenReceived.getTokenType() + " " + lastTokenReceived.getAccessToken();
                }
                logger.debug("Token expired. Calling login API ");
                lastTokenReceived = null;
            }
            HttpClient client = new HttpClient(timeout);
            if (isProxyRequired) {
                client.setDefaultProxy();
            }
            client.setPayloadLoggerInterceptor((HttpRequestSnapshot httpRequestSnapshot) -> httpRequestSnapshot.body.replaceAll("\"password\":\"[^\"]*\"", "\"password\":\"****\""));
            lastTokenReceived = client.postForEntity(loginUrl, Json.toJson(tokenRequest), new HashMap<>() {{
                put("Content-Type", "application/json");
            }}, DescoPostpaidTokenResponse.class);

            if (lastTokenReceived == null) {
                logger.error("Unable to obtain token.");
                throw new Exception("Unable to obtain token.");
            }
            if (!"ok".equals(lastTokenReceived.getStatus())) {
                logger.debug(lastTokenReceived.getMessage());
                throw new Exception(lastTokenReceived.getMessage());
            } else {
                isTokenExpired = false;
                lastTokenReceived.setExpirationTimeInMilli(new Date().getTime() + expireDuration);
                return lastTokenReceived.getTokenType() + " " + lastTokenReceived.getAccessToken();
            }
        }
    }

    public boolean isTokenExpired() {
        return isTokenExpired;
    }

    public void setTokenExpired(boolean isTokenExpired) {
        this.isTokenExpired = isTokenExpired;
    }
}