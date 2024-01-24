package com.grameenphone.wipro.fmfs.cbp.model.view.report;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Category;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;

import java.util.List;

@JsonExcludeNestedProps({"categories.companies.category", "clients.clientDivisions"})
public class FilterData {
    public List<Category> categories;
    public List<Client> clients;
}