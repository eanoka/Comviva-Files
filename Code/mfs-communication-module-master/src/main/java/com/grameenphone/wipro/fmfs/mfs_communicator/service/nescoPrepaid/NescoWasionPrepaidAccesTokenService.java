package com.grameenphone.wipro.fmfs.mfs_communicator.service.nescoPrepaid;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid.NescoWasionTokenRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.nescoPrepaid.NescoWasionTokenResponse;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.HttpClient.HttpRequestSnapshot;
import com.grameenphone.wipro.utility.marshal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;

@Service
public class NescoWasionPrepaidAccesTokenService {
    protected static final Logger logger = LoggerFactory.getLogger(NescoWasionPrepaidAccesTokenService.class);
    public static NescoWasionTokenRequest tokenRequest;
    private static volatile NescoWasionTokenResponse lastTokenReceived;
    
	@Value("${nesco_wasion_prepaid_request_timeout}")
	int timeout;
    @Value("${nesco_wasion_prepaid_app_name}")
    String appName;
    @Value("${nesco_wasion_prepaid_app_password}")
    String appPassword;
    @Value("${nesco_wasion_prepaid_proxy_required}")
    Boolean isProxyRequired;
    @Value("${nesco_wasion_prepaid_login_url}")
    String loginUrl;
    @Value("${nesco_wasion_token_expiration_duration_in_millisecond}")
    long expireDuration;

    @PostConstruct
    private void initialize() {
        tokenRequest = new NescoWasionTokenRequest();
        tokenRequest.app_name = appName;
        tokenRequest.app_password = appPassword;
    }

    public String getToken() throws Exception {
        synchronized (NescoWasionPrepaidAccesTokenService.class) {
            if (lastTokenReceived != null) {
                if (lastTokenReceived.getExpirationTimeInMilli() - 600000 >= new Date().getTime()) { //10 min deducted for time safety
                    logger.debug("Reusing stored token: (" + lastTokenReceived.data + "): " + lastTokenReceived.data.access_token);
                    return lastTokenReceived.data.access_token;
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
            }}, NescoWasionTokenResponse.class);
            
            if(lastTokenReceived == null) {
            	logger.error("Unable to obtain token.");
            	throw new Exception("Unable to obtain token.");
            }
            if(!"0".equals(lastTokenReceived.resultcode)) {
            	logger.debug(lastTokenReceived.resultdesc);
            	throw new Exception(lastTokenReceived.resultdesc);
            }else {
            	lastTokenReceived.setExpirationTimeInMilli(new Date().getTime() + expireDuration);
                return lastTokenReceived.data.access_token;
			}
		}
    }
}