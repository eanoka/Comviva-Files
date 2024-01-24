package com.grameenphone.wipro.fmfs.cbp.model.view.payment_request;

import java.util.List;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.BillDetailTask;

@JsonExcludeNestedProps({"records.updatedBy.role", "records.updatedBy.client", "records.updatedBy.clientDivisions", "records.updatedBy.actions", "records.company.category.companies", "records.clientDivision.client.clientDivisions", "records.modifiedDataFor.addedBy", "records.modifiedDataFor.updatedBy", "records.modifiedDataFor.company.category.companies", "records.modifiedDataFor.clientDivision.client"})
public class PaginatedBillDetailTask {
    public long count;
    public long offset = 0;
    public int perPage = 10;
    public List<BillDetailTask> records;
}