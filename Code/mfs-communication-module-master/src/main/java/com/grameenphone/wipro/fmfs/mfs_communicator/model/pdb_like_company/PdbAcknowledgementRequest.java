package com.grameenphone.wipro.fmfs.mfs_communicator.model.pdb_like_company;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "xml")
public class PdbAcknowledgementRequest extends PdbBaseRequest {
    public String vendingMode;
    public String refCode;
}
