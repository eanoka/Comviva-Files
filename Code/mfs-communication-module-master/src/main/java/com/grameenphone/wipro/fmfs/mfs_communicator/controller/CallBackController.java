package com.grameenphone.wipro.fmfs.mfs_communicator.controller;

import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.paybill_sub_requests.PayBillCallBackRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.PayBillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/callback")
public class CallBackController {

    private static final Logger logger = LoggerFactory.getLogger(CallBackController.class);

    @Autowired
    @Qualifier("PayBillService")
    PayBillService service;
    @PostMapping("/update/payment")
    public ResponseEntity<?> paymentStatusReceiver(@RequestBody PayBillCallBackRequest request){
        try{
            service.updatePayBillStatus(request);
            return new ResponseEntity<>(HttpStatus.OK);
        }catch (Exception e){
            logger.error("Error while updating the payment status on database: "+e);
            return new ResponseEntity<>(new ValidationException("ERROR WHILE UPDATING PAYMENT STATUS:"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
