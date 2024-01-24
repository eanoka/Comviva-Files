package com.grameenphone.wipro.utility.common;


import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.sms.SmsAccesInfo;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.sms.SmsInfo;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.sms.Charge;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.sms.DPDPSMSSendRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.sms.Message;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.sms.MessageInfo;
import com.grameenphone.wipro.utility.marshal.Json;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class SmsUtil {
    protected static final Logger logger = LoggerFactory.getLogger(SmsUtil.class);

    static String accessKey;
    static String accesschannel;
    static String servicekey;
    static String serviceIdentifier;
    static String smsLanguage;
    static String senderId;
    static String msgType;
    static String validity;
    static String deliveryReport;
    static String baseUrl;
    static String user;
    static String password;
    static String chargeCode;

    static {
        accessKey = Application.environment.getProperty("sms_accesskey");
        accesschannel = Application.environment.getProperty("sms_accesschannel");
        servicekey = Application.environment.getProperty("sms_servicekey");
        serviceIdentifier = Application.environment.getProperty("sms_serviceIdentifier");
        smsLanguage = Application.environment.getProperty("sms_language");
        senderId = Application.environment.getProperty("sms_senderId");
        msgType = Application.environment.getProperty("sms_msgType");
        validity = Application.environment.getProperty("sms_validity");
        deliveryReport = Application.environment.getProperty("sms_deliveryReport");
        baseUrl = Application.environment.getProperty("sms_base_url");
        user = Application.environment.getProperty("sms_user");
        password = Application.environment.getProperty("sms_password");
        chargeCode = Application.environment.getProperty("sms_serviceIdentifier_charge_code");
    }

    public static void sendSms(final String to_no, final String body, final String banglaSmsBody, boolean isAsync) {
        Thread thread = new Thread(() -> {
            String[] toNoList;
            if (to_no.contains(",")) {
                toNoList = to_no.split(",");
            } else {
                toNoList = new String[1];
                toNoList[0] = to_no;
            }

            for (String number : toNoList) {
                number = "880" + StringUtil.sanitizeMsisdn(number);
                try {
                    SmsAccesInfo accesInfo = new SmsAccesInfo();
                    accesInfo.setAccesskey(accessKey);
                    accesInfo.setEndUserId(number);
                    accesInfo.setAccesschannel(accesschannel);
                    String randomNumb = RandomStringUtils.randomNumeric(15);
                    accesInfo.setReferenceCode("{" + randomNumb + "}");
                    accesInfo.setServicekey(servicekey);
                    accesInfo.setServiceIdentifier(serviceIdentifier);
                    
                    accesInfo.setUser(user);
                    accesInfo.setPassword(password);
                    
                    Charge charge=new Charge(); 
                    charge.setCode(chargeCode);
                    charge.setAmount("1");
                    charge.setDescription("GPAY-GP");
                    charge.setTaxAmount("0");
                    charge.setCurrency("BDT");
                    
                    SmsInfo smsInfo = new SmsInfo();
                    smsInfo.setMsgTransactionId(getDateToStrDDMM(new Date()) + "_" + randomNumb);
                    smsInfo.setLanguage(smsLanguage);

                    smsInfo.setSenderId(senderId);
                    //Below code commented to make it enabled for further english sms send
                    smsInfo.setMessage(body);
                    //smsInfo.setMessage(banglaSmsBody);
                    smsInfo.setMsgType(msgType);
                    smsInfo.setValidity(validity);
                    smsInfo.setDeliveryReport(deliveryReport);
                    
                    MessageInfo messageInfo=new MessageInfo();
                    messageInfo.setDeliveryReport("1");
                    messageInfo.setMsgTransactionId(getDateToStrDDMM(new Date()) + "_" + randomNumb);
                    messageInfo.setSenderId(senderId);
                    messageInfo.setValidity("1");
                    
                    Message messageen=new Message(); 
                    messageen.setLanguage("BN");
                    messageen.setMsgType("unicode");
                    messageen.setMessage(banglaSmsBody);
                    
                    Message messagebn=new Message();
                    messagebn.setLanguage("EN");
                    messagebn.setMsgType("text");
                    messagebn.setMessage(body); 
                    
                    List<Message> messageList=new ArrayList<Message>();  
                    messageList.add(messagebn);
                    messageList.add(messageen);                   
                    messageInfo.setMessage(messageList);
                    

                    DPDPSMSSendRequest sendRequest = new DPDPSMSSendRequest();
                    sendRequest.setAccesInfo(accesInfo);
                    sendRequest.setCharge(charge);
                    sendRequest.setSmsInfo(smsInfo);
                    sendRequest.setMessageInfo(messageInfo);
                    
                    HttpClient httpClient = new HttpClient(null);
                    httpClient.setCharset("UTF-8");
                    String bodyGson = Json.toJson(sendRequest);
                    httpClient.post(baseUrl, bodyGson, new HashMap<>() {{
                        put("accept", "application/json");
                        put("Content-Type", "application/json");
                    }});
                } catch (Throwable ex) {
                    logger.error("Error sending sms for msisdn " + to_no, ex);
                }
            }
        });
        if (isAsync) {
            logger.debug("SMS is being sent through thread# [" + thread.getName() + "] [" + thread.getId() + "]");
            thread.start();
        } else {
            thread.run();
        }
    }

    public static String getDateToStrDDMM(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("ddMM");
        try {
            return dateFormat.format(date);
        } catch (Exception ex) {
            return null;
        }
    }
}