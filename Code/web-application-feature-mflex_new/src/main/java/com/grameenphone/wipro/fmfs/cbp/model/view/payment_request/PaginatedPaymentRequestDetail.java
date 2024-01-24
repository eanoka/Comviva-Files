package com.grameenphone.wipro.fmfs.cbp.model.view.payment_request;

import java.util.List;

public class PaginatedPaymentRequestDetail {
    public long count;
    public long offset = 0;
    public int perPage = 10;
    public List<RequestDetail> records;
}