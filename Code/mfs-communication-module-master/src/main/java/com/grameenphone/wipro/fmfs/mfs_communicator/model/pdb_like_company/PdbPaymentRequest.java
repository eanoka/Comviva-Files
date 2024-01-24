package com.grameenphone.wipro.fmfs.mfs_communicator.model.pdb_like_company;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "xml")
@JsonPropertyOrder({"userName", "userPass", "transId", "meterNo", "calcMode", "amount", "verifyCode", "verifyData"})
public class PdbPaymentRequest extends PdbBaseRequest {
    @JacksonXmlProperty(localName = "calcMode", isAttribute = true)
    public String calcMode;

    @JacksonXmlProperty(localName = "verifyCode", isAttribute = true)
    public String verifyCode;

    @JacksonXmlProperty(localName = "verifyData", isAttribute = true)
    public String verifyData;
}