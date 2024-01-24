package com.grameenphone.wipro.fmfs.cbp.model.view.report;

import java.util.List;
import java.util.Map;

public class PaginatedReportData {
    public Long offset;
    public Long total;
    public List<Map<String, Object>> records;
}