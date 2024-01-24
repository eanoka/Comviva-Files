package com.grameenphone.wipro.fmfs.mfs_communicator.event;

import com.grameenphone.wipro.fmfs.mfs_communicator.service.MFSService;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.marshal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class BillPayEventHandler {
    protected static final Logger logger = LoggerFactory.getLogger(BillPayEventHandler.class);

    @Value("${event_queue_manager_url}")
    String event_queue_manager_url;

    @Autowired
    MFSService mfsService;

    @Async
    @EventListener(BillPayEvent.class)
    public void handleBillPayEvent(BillPayEvent event) {
        logger.debug("Enqueuing bill pay event for bill paid by " + event.initiator + " for " + event.initiator_on_behalf);
        //grade is being set here to process this time cost activity in asynchronous block
        try {
        	event.initiator_grade = mfsService.getUserGrade(event.initiator);
        	HttpClient client = new HttpClient();
        
            client.post(event_queue_manager_url, Json.toJson(event), new HashMap<>() {{
                put("Content-Type", "application/json");
            }});
        } catch (Throwable e) {
            logger.error("Couldn't raised event", e);
        }
    }
}