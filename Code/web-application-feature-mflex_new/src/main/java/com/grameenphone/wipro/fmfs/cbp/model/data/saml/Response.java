package com.grameenphone.wipro.fmfs.cbp.model.data.saml;

import java.util.Map;

public class Response {
    public String ID;
    public String Version;
    public String IssueInstant;
    public String Destination;
    public String InResponseTo;
    public String Issuer;
    public Map Signature;
    public Map Status;
    public Assertion Assertion;
}