package com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "COMMAND")
public class SubscriberPayToBillerRequest extends PayToBillerRequest {
    @JacksonXmlProperty(localName = "LANGUAGE1")
    public String language = "1";
    @JacksonXmlProperty(localName = "USERTYPE")
    public String userType = "SUBSCRIBER";
    @JacksonXmlProperty(localName = "BILLRECORD")
    public String billRecord = "RECORD_NUMBER";

    {
        type = "CPMBREQ";
        pref3 = "Y";
    }
}