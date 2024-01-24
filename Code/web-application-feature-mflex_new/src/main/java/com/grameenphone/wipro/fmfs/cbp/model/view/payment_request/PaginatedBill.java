package com.grameenphone.wipro.fmfs.cbp.model.view.payment_request;

import com.grameenphone.wipro.annot.JsonExcludeNestedProps;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Bill;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.PaymentRequest;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.PaymentRequestHop;

import java.util.List;

@JsonExcludeNestedProps({"records.addedBy", "records.updatedBy", "records.company.category.companies", "records.clientDivision.client.clientDivisions", "records.request", "request.account.clientDivisions"})
public class PaginatedBill {
    public long count;
    public long offset = 0;
    public int perPage = 10;
    public List<Bill> records;
    public RequestSummary request;
    public List<PaymentRequestHop> hops;
}