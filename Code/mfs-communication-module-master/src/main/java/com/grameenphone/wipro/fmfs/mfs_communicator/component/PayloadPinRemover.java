package com.grameenphone.wipro.fmfs.mfs_communicator.component;

import org.springframework.stereotype.Component;

@Component
public class PayloadPinRemover {
    public String sanitizedPayload(String payload) {
        return payload.replaceAll("(\"pin\")\\s*:\\s*\"[^\"]*\"|(\"password\")\\s*:\\s*\"[^\"]*\"", "$1$2: \"****\"");
    }
}