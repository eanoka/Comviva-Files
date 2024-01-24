package com.grameenphone.wipro.fmfs.mfs_communicator.service;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.Bill;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.DueBills;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.MfsResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.summary.BillData;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillFetcher;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;

public abstract class SummaryDbBasedCompanyService implements BillFetcher<DueBills>, BillPayer<PaymentState, Void> {
    protected String companyCode;

    @Value("${mfs.datasource.default.schema}")
    private String defaultSchema;

    @Override
    public String getCategory() {
        return "ELEC POST";
    }

    @Override
    public DueBills fetchDueBills(String accountNo, String msisdn, WalletType wallet_type, Channel channel, Map params) {
    	Date dueDateMargin = DateTime.now().withTime(LocalTime.MIDNIGHT).minusMillis(1).toDate(); // Last millisecond of yesterday
        String billQuery = "select B.COMPANY_CODE as companyCode, B.ACCOUNT_NUMBER as accountNumber, B.BILL_NUMBER as billNumber, B.BILL_MONTH as month, B.BILL_DATE as dueDate, B.AMOUNT as amount, B.STATUS as status, B.VAT as vat from " + defaultSchema + ".MNY_BILL_UPLOAD B where company_code=? and account_number=? and B.STATUS = ?";
        List<BillData> rows = null;
        if (companyCode.equalsIgnoreCase("DSCO")) {
            rows = mfsService.executeQuery(billQuery, BillData.class, companyCode, accountNo, 'N');
        } else {
            billQuery = billQuery + " and BILL_DATE > ?";
            rows = mfsService.executeQuery(billQuery, BillData.class, companyCode, accountNo, 'N', dueDateMargin);
        }
        DueBills bills = new DueBills();
        bills.company = companyCode;
        bills.consumerId = accountNo;
        rows.forEach(row -> {
            Bill bill = new Bill();
            bill.amount = row.amount;
            bill.billDueDate = row.dueDate;
            bill.billMonthYear = row.month.substring(4) + "-" + row.month.substring(0, 4);
            bill.billNo = row.billNumber;
            bill.vat = row.vat;
            bill.serviceCharge = mfsService.getServiceCharge(msisdn, companyCode, row.amount, wallet_type, channel);
            bills.bills.add(bill);
        });
        return bills;
    }

    @Override
    public PaymentResult convertToGeneric(PaymentState paymentState, Void _null, PaymentRequest request) {
        PaymentResult result = new PaymentResult();
        MfsResponse mfsResponse = paymentState.mfsPaymentResponse;
        result.txnId = mfsResponse.txnid;
        result.status = BillPayStatus.SUCCESS;
        result.message = mfsResponse.message;
        return result;
    }

    @Override
    public Void pay(PaymentState state, PaymentRequest request, String mfsChannel) {
        state.billPayServiceStatus.setStatus(BillPayStatus.SUCCESS);
        billPayServiceStatusRepository.save(state.billPayServiceStatus);
        return null; //There is no service specific payment
    }
}