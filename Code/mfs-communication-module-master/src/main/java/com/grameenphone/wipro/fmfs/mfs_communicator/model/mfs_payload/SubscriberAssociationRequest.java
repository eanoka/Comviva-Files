package com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "COMMAND")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriberAssociationRequest extends AssociationRequest {
    {
        type = "BPREGREQ";
    }
}