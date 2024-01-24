package com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "COMMAND")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetailerAssociationRequest extends AssociationRequest {
    @JacksonXmlProperty(localName = "PROVIDER2")
    public Integer provider2 = 101;
    @JacksonXmlProperty(localName = "LANGUAGE1")
    public Integer language1 = 1;
    @JacksonXmlProperty(localName = "LANGUAGE2")
    public Integer language2 = 1;
    @JacksonXmlProperty(localName = "MSISDN2")
    public String custMsisdn;
    @JacksonXmlProperty(localName = "FLAG")
    public String flag = "Add";

    {
        type = "RBPREGAREQ";
    }
}