package com.grameenphone.wipro.fmfs.cbp.model.view.billdata;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Category;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.ClientDivision;

import java.util.Collection;
import java.util.List;

@JsonExcludeNestedProps({"divisions.client"})
public class EditPageFilterData {
    public Collection<ClientDivision> divisions;
    public List<Category> categories;
}