package com.grameenphone.wipro.fmfs.mfs_communicator.controller;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.DueBillResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grameenphone.wipro.annot.PinContainingRequest;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.AmountValidationResponse.AmountValidationResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ConsumerValidateRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ConsumerValidationResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ConsumerValidationResponse.ConsumerValidationResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.DueBills;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.GetUnpaidBillDetailResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.GetUnpaidBillDetailResponse.BillDetail;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ServiceCharge;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ServiceChargeResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.ServiceChargeResponse.ServiceChargeResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.MFSService;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.ServiceFinder;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.AmountValidator;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillFetcher;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.ConsumerValidator;
import com.grameenphone.wipro.utility.common.PayloadUtil;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.spring.ContextUtil;

@RestController
public class BillController {
    @Autowired
    MFSService mfsService;
    
    @Autowired
    ServiceFinder serviceFinder;

    public DueBillResponse dues(@RequestBody DueBillsRequest request) throws HttpErrorResponseException {
    	BillFetcher billFetcher = serviceFinder.getServiceWithPrefix("Postpaid", "_Bill_Fetcher", BillFetcher.class);
        if (StringUtil.isNullOrEmpty(request.getConsumerId())) {
            throw new ValidationException(422, "Consumer Id can not be empty");
        }
        DueBillResponse dueBills = billFetcher.fetchDueBillList(request);
        if (dueBills == null) {
            throw new HttpErrorResponseException(500, "Unable To Collect Due Bills");
        }
        return dueBills;
    }

    @PinContainingRequest
    public PaymentResponse payment(@RequestBody PaymentRequest request) throws HttpErrorResponseException {
        BillPayer billPayer = serviceFinder.getServiceWithPrefix("Postpaid", "_Bill_Payer", BillPayer.class);
        if (billPayer == null) {
            throw new ValidationException(request.getCompany() + " Is not a valid or supported company");
        }
        if (request.wallet_type == WalletType.RET) {
            String _10DigitCustomer;
            if (request.customer == null || (_10DigitCustomer = StringUtil.sanitizeMsisdn(request.customer)) == null) {
                throw new ValidationException("Invalid Customer Wallet MSISDN");
            }
            request.customer = "0" + _10DigitCustomer;
        }
        if (StringUtil.isNullOrEmpty(request.pin)) {
            throw new ValidationException("PIN must be specified");
        }
        if (request.amount == 0) {
            throw new ValidationException("Amount can not be 0");
        }
        if (StringUtil.isNullOrEmpty(request.getConsumerId())) {
            throw new ValidationException("Consumer Id can not be empty");
        }
        if (StringUtil.isNullOrEmpty(request.bill)) {
            throw new ValidationException("Bill no can not be empty");
        }
        return PayloadUtil.wrapResponse(new PaymentResponse(), billPayer.payBill(request));
    }

    @RequestMapping("/bill/validate/consumer")
    public ConsumerValidationResponse validateConsumer(@RequestBody ConsumerValidateRequest request) throws HttpErrorResponseException {
        if (StringUtil.isNullOrEmpty(request.consumerId)) {
            throw new ValidationException("Consumer Id can not be empty");
        }
        if (StringUtil.isNullOrEmpty(request.company)) {
            throw new ValidationException("Company can not be empty");
        }
        ConsumerValidator validator = serviceFinder.getServiceWithPrefix(request.company, "_Consumer_Validator", ConsumerValidator.class);
        if (validator == null) {
            throw new ValidationException(request.company + " is not a valid or supported company");
        }
        ConsumerValidationResult result = validator.validateConsumer(request);
        return PayloadUtil.wrapResponse(new ConsumerValidationResponse(), result);
    }

    @RequestMapping("/bill/validate/amount")
    public AmountValidationResponse validateAmount(@RequestBody AmountValidationRequest request) throws HttpErrorResponseException {
        if (StringUtil.isNullOrEmpty(request.consumerId)) {
            throw new ValidationException("Consumer Id can not be empty");
        }
        if (StringUtil.isNullOrEmpty(request.company)) {
            throw new ValidationException("Company can not be empty");
        }
        AmountValidator validator = serviceFinder.getServiceWithPrefix(request.company, "_Amount_Validator", AmountValidator.class);
        
        if (validator == null) {
            throw new ValidationException(request.company + " is not a valid or supported company");
        }
        AmountValidationResult result = validator.validateAmount(request);
        return PayloadUtil.wrapResponse(new AmountValidationResponse(), result);
    }

    @RequestMapping("/bill/service_charge")
    public ServiceChargeResponse validateServicecharge(@RequestBody ServiceCharge request) throws HttpErrorResponseException {
        if (StringUtil.isNullOrEmpty(request.company)) {
            throw new ValidationException("Company can not be empty");
        }
        if (request.amount == 0) {
            throw new ValidationException("Amount can not be 0");
        }
        if (request.msisdn == null || (request.msisdn = StringUtil.sanitizeMsisdn(request.msisdn)) == null) {
            throw new ValidationException("Invalid Customer Wallet MSISDN");
        }
        ServiceChargeResult result = new ServiceChargeResult();
        result.service_charge = mfsService.getServiceCharge(request.msisdn, request.company, request.amount, request.wallet_type, request.channel);
        return PayloadUtil.wrapResponse(new ServiceChargeResponse(), result);
    }
    
    @RequestMapping("/bill/detail")
    public GetUnpaidBillDetailResponse fetchUnpaidBillDetail(@RequestBody DueBillsRequest request) throws HttpErrorResponseException {
    	BillFetcher billFetcher = ContextUtil.getBean(request.getCompany() + "_Bill_Fetcher", BillFetcher.class);
        if (billFetcher == null) {
            throw new ValidationException(request.getCompany() + " Is not a valid or supported company");
        }
        if (StringUtil.isNullOrEmpty(request.getConsumerId())) {
            throw new ValidationException("Consumer Id can not be empty");
        }
        BillDetail billList = billFetcher.getBillInfo(request.getConsumerId(), request.msisdn, request.wallet_type, request.channel, request.params);
        if (billList == null) {
            throw new HttpErrorResponseException(500, "Unable To Collect Due Bills");
        }
        return PayloadUtil.wrapResponse(new GetUnpaidBillDetailResponse(), billList);
    }
}