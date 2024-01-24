package com.grameenphone.wipro.fmfs.mfs_communicator.controller;

import com.grameenphone.wipro.annot.PinContainingRequest;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.BalanceResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PinVerificationRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PinVerificationResponse;
import com.grameenphone.wipro.utility.common.PayloadUtil;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.MFSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
public class WalletController {
	@Autowired
	MFSService mfsService;

	@GetMapping("/balance/{channel}/{wallet_type}/{msisdn}")
	public BalanceResponse balance(@PathVariable String msisdn) {
		BalanceResponse.Balance balance = mfsService.getBalanceInfo(msisdn);
		return PayloadUtil.wrapResponse(new BalanceResponse(), balance);
	}

	@PinContainingRequest
	@PostMapping("/pin/verify")
	public PinVerificationResponse verifyPin(@RequestBody PinVerificationRequest request) throws HttpErrorResponseException {
		if (StringUtil.isNullOrEmpty(request.pin)) {
			throw new ValidationException("Pin can not be empty");
		}
		if (StringUtil.isNullOrEmpty(request.msisdn)) {
			throw new ValidationException("MSISDN can not be empty");
		}

	//	PinVerificationResponse.Result balance = mfsService.verifyPin(request.msisdn, request.wallet_type, request.pin);
		PinVerificationResponse.Result balance = mfsService.verifyPin(request);
		return PayloadUtil.wrapResponse(new PinVerificationResponse(), balance);
	}
}