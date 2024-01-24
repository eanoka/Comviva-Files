package com.grameenphone.wipro.fmfs.cbp.model.view.billdata;

import io.github.millij.poi.ss.model.annotations.SheetColumn;

public class BulkBillDataExcelRow {
	@SheetColumn("NAME")
	private String name;
	
    @SheetColumn("SUB_ACCOUNT")
    private String subAccountName;

    @SheetColumn("BILL_CATEGORY")
    private String categoryName;

    @SheetColumn("BILL_COMPANY")
    private String companyName;

    @SheetColumn("ACCOUNT_NO")
    private String accountNo;

    @SheetColumn("MSISDN")
    private String msisdn;

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    @SheetColumn("ADDITIONAL_FIELD_NAME_1")
    private String additionalFieldName1;
    
    @SheetColumn("ADDITIONAL_FIELD_VALUE_1")
    private String additionalFieldValue1;
    
    @SheetColumn("ADDITIONAL_FIELD_NAME_2")
    private String additionalFieldName2;
    
    @SheetColumn("ADDITIONAL_FIELD_VALUE_2")
    private String additionalFieldValue2;
    
    @SheetColumn("ADDITIONAL_FIELD_NAME_3")
    private String additionalFieldName3;
    
    @SheetColumn("ADDITIONAL_FIELD_VALUE_3")
    private String additionalFieldValue3;

    public String getSubAccountName() {
        return subAccountName;
    }

    public void setSubAccountName(String subAccountName) {
        this.subAccountName = subAccountName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(String accountNo) {
        this.accountNo = accountNo;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

	public String getAdditionalFieldName1() {
		return additionalFieldName1;
	}

	public void setAdditionalFieldName1(String additionalFieldName1) {
		this.additionalFieldName1 = additionalFieldName1;
	}

	public String getAdditionalFieldValue1() {
		return additionalFieldValue1;
	}

	public void setAdditionalFieldValue1(String additionalFieldValue1) {
		this.additionalFieldValue1 = additionalFieldValue1;
	}

	public String getAdditionalFieldName2() {
		return additionalFieldName2;
	}

	public void setAdditionalFieldName2(String additionalFieldName2) {
		this.additionalFieldName2 = additionalFieldName2;
	}

	public String getAdditionalFieldValue2() {
		return additionalFieldValue2;
	}

	public void setAdditionalFieldValue2(String additionalFieldValue2) {
		this.additionalFieldValue2 = additionalFieldValue2;
	}

	public String getAdditionalFieldName3() {
		return additionalFieldName3;
	}

	public void setAdditionalFieldName3(String additionalFieldName3) {
		this.additionalFieldName3 = additionalFieldName3;
	}

	public String getAdditionalFieldValue3() {
		return additionalFieldValue3;
	}

	public void setAdditionalFieldValue3(String additionalFieldValue3) {
		this.additionalFieldValue3 = additionalFieldValue3;
    }
}