package com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "COMMAND")
public class RetailerPayToBillerRequest extends PayToBillerRequest {
    @JacksonXmlProperty(localName = "TRID")
    public String trId = "TR_ID";

    {
        type = "RPMBREQ";
        pref3 = "N";
    }
}