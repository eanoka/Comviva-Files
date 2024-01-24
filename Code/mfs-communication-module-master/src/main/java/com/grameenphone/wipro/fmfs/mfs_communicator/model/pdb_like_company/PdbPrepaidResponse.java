package com.grameenphone.wipro.fmfs.mfs_communicator.model.pdb_like_company;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "Result")
public class PdbPrepaidResponse {
    @JacksonXmlProperty(localName = "state", isAttribute = true)
    public String state;

    @JacksonXmlProperty(localName = "transID", isAttribute = true)
    public String transID;

    @JacksonXmlProperty(localName = "transTime", isAttribute = true)
    public String transTime;

    @JacksonXmlProperty(localName = "refCode", isAttribute = true)
    public String refCode;

    @JacksonXmlProperty(localName = "meterNum", isAttribute = true)
    public String meterNum;

    @JacksonXmlProperty(localName = "customerName", isAttribute = true)
    public String customerName;

    @JacksonXmlProperty(localName = "tariffCode", isAttribute = true)
    public String tariffCode;

    @JacksonXmlProperty(localName = "vendAMT", isAttribute = true)
    public String vendAMT;

    @JacksonXmlProperty(localName = "arrearAMT", isAttribute = true)
    public String arrearAMT;

    @JacksonXmlProperty(localName = "feeAMT", isAttribute = true)
    public String feeAMT;

    @JacksonXmlProperty(localName = "engAMT", isAttribute = true)
    public String engAMT;

    @JacksonXmlProperty(localName = "token", isAttribute = true)
    public String token;

    @JacksonXmlProperty(localName = "message", isAttribute = true)
    public String message;

    @JacksonXmlProperty(localName = "seq", isAttribute = true)
    public String seq;

    @JacksonXmlProperty(localName = "meterType", isAttribute = true)
    public String meterType;

    @JacksonXmlProperty(localName = "fee")
    public Fee fee;

    public static class Fee {
        @JacksonXmlProperty(localName = "item")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<Item> items;
    }

    public static class Item {
        @JacksonXmlProperty(isAttribute = true)
        public String id;
        @JacksonXmlProperty(isAttribute = true)
        public String name;
        @JacksonXmlProperty(isAttribute = true)
        public String amt;
    }
}