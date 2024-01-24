package com.grameenphone.wipro.fmfs.cbp.model.view.user;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.ClientDivision;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;

import java.util.List;

@JsonExcludeNestedProps({"divisions.client", "clients.clientDivisions", "roles.actions", "roles.inheritedFrom", "roles.client", "user.client.clientDivisions", "user.actions", "user.clientDivisions.client"})
public class CreateFormInitializationDataResponse {
    public List<Client> clients;
    public List<ClientDivision> divisions;
    public List<Role> roles;
    public User user;
}