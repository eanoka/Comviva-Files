package com.grameenphone.wipro.fmfs.cbp.model.data.session;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;

import java.util.ArrayList;
import java.util.List;

@JsonExcludeNestedProps({"client.clientDivisions", "role.client", "role.actions", "role.inheritedFrom.client", "role.inheritedFrom.actions"})
public class FrontEndUser {
    public String session;
    public String name;
    public Long id;
    public String email;
    public String adid;
    public List<String> permissions = new ArrayList<>();
    public boolean isGP;
    public Client client;
    public Role role;
}