package com.grameenphone.wipro.fmfs.mfs_communicator.model.pdb_like_company;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class PdbBaseRequest {
    @JacksonXmlProperty(localName = "userName", isAttribute = true)
    public String userName;

    @JacksonXmlProperty(localName = "userPass", isAttribute = true)
    public String userPass;

    @JacksonXmlProperty(localName = "transID", isAttribute = true)
    public String transId;

    @JacksonXmlProperty(localName = "meterNum", isAttribute = true)
    public String meterNo;

    @JacksonXmlProperty(localName = "amount", isAttribute = true)
    public int amount;
}