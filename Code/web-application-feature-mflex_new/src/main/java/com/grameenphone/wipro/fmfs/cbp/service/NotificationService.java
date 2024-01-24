package com.grameenphone.wipro.fmfs.cbp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.fmfs.cbp.model.data.websocket.SocketMessage;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.marshal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.stereotype.Service;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

@Service
public class NotificationService {
    private final static Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    MessageSendingOperations<String> messagingTemplate;

    @Value("${email-notification-from}")
    String mailFrom;

    @Value("${email-notification-host}")
    String mailHost;

    public void notifyUser(String receiver, SocketMessage message) throws JsonProcessingException {
        messagingTemplate.convertAndSend("/socket/notifier/" + receiver + "/notify", Json.toJson(message));
    }

    public void sendEmail(String subject, String messageBody, String to) {
        sendEmail(subject, messageBody, to, false);
    }

    public void sendEmail(String subject, String messageBody, String to, boolean asynchronous) {
        sendEmail(subject, messageBody, to, null, null, asynchronous);
    }

    public void sendEmail(String subject, String messageBody, String to, String cc, boolean asynchronous) {
        sendEmail(subject, messageBody, to, cc, null, asynchronous);
    }

    public void sendEmail(String subject, String messageBody, String to, String cc, String bcc, boolean asynchronous) {
        sendEmail(subject, messageBody, to, cc, bcc, null, asynchronous);
    }

    public void sendEmail(String subject, String messageBody, String to, String cc, String bcc, MimeBodyPart attachment, boolean asynchronous) {
        if(StringUtil.isNullOrEmpty(to)) {
            return;
        }
        Thread x = new Thread(() -> {
            Properties properties = new Properties();
            properties.setProperty("mail.smtp.host", mailHost);
            try {
                Session session = Session.getDefaultInstance(properties);
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(mailFrom));
                message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                if(!StringUtil.isNullOrEmpty(cc)) {
                    message.addRecipients(RecipientType.CC, InternetAddress.parse(cc));
                }
                if(!StringUtil.isNullOrEmpty(bcc)) {
                    message.addRecipients(RecipientType.BCC, InternetAddress.parse(bcc));
                }
                message.setSubject(subject);
                BodyPart messageBodyPart = new MimeBodyPart();
                messageBodyPart.setContent(messageBody, "text/html; charset=utf-8");
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);
                if(attachment != null) {
                    multipart.addBodyPart(attachment);
                }
                message.setContent(multipart);
                Transport.send(message);
                logger.debug("An email has been sent to " + to + " with subject " + subject);
            } catch (MessagingException mex) {
                logger.error("Couldn't send notification", mex);
            }
        });
        logger.debug("Sending email to " + to + " with subject " + subject);
        if (asynchronous) {
            logger.debug("Email is being sent in thread " + x.getId() + ":" + x.getName());
            x.start();
        } else {
            x.run();
        }
    }
}