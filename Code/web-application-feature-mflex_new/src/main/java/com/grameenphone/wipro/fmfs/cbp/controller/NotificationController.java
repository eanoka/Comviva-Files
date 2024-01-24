package com.grameenphone.wipro.fmfs.cbp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.annot.AllowAnonymous;
import com.grameenphone.wipro.fmfs.cbp.model.data.websocket.SocketMessage;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillDetailTask;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.PaymentRequest;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@AllowAnonymous
@RestController
public class NotificationController {
    @Autowired
    NotificationService notificationService;

    public String billCollectionComplete(long rid) throws JsonProcessingException {
        Long userId = (Long)CrudDao.get(BillDetailTask.class).query().eq("id", rid).selectOne("addedBy.id");
        SocketMessage socketMessage = new SocketMessage();
        socketMessage.message = "Bill Collection #" + String.format("%08x", rid).toUpperCase() + " Complete.";
        socketMessage.topic = "BCR_COMPLETE";
        socketMessage.data = rid;
        notificationService.notifyUser("user-" + userId, socketMessage);
        return "OK";
    }

    public String paymentComplete(long rid) throws JsonProcessingException {
        Long userId = (Long)CrudDao.get(PaymentRequest.class).query().eq("id", rid).selectOne("addedBy.id");
        SocketMessage socketMessage = new SocketMessage();
        socketMessage.message = "Payment For #" + String.format("%08x", rid).toUpperCase() + " Done.";
        socketMessage.topic = "PR_COMPLETE";
        socketMessage.data = rid;
        notificationService.notifyUser("user-" + userId, socketMessage);
        return "OK";
    }
}