package com.grameenphone.wipro.utility;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

public class NetworkUtil {
    public static String resolveIpFromDomainName(String hostname, String dnsServer) throws NamingException {
        final String[] DNSTYPE = new String[] { "A" };
        final Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns://" + dnsServer + "/.");
        final DirContext ictx = new InitialDirContext(env);
        final Attributes attr = ictx.getAttributes(hostname, DNSTYPE);
        return (String) attr.get("A").get(0);
    }
}