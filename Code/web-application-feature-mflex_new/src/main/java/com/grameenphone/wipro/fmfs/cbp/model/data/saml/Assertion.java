package com.grameenphone.wipro.fmfs.cbp.model.data.saml;

import java.util.List;
import java.util.Map;

public class Assertion {
    public String ID;
    public String Version;
    public String IssueInstant;
    public String Issuer;
    public Map Signature;
    public Map Subject;
    public Map Conditions;
    public Map AuthnStatement;
    public List<Attribute> AttributeStatement;
}