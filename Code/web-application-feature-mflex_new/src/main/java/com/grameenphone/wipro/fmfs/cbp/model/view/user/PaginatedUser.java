package com.grameenphone.wipro.fmfs.cbp.model.view.user;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;

import java.util.List;

@JsonExcludeNestedProps({"records.role.inheritedFrom", "records.role.actions", "records.role.client", "records.clientDivisions", "records.actions"})
public class PaginatedUser {
    public long count;
    public long offset = 0;
    public int perPage = 10;
    public List<User> records;
}