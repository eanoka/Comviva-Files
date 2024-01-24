package com.grameenphone.wipro.fmfs.cbp.model.view.role;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;

import java.util.List;

@JsonExcludeNestedProps({"records.inheritedFrom.inheritedFrom", "records.inheritedFrom.actions", "records.inheritedFrom.client", "records.actions", "records.client"})
public class PaginatedRole {
    public long count;
    public long offset = 0;
    public int perPage = 10;
    public List<Role> records;
}