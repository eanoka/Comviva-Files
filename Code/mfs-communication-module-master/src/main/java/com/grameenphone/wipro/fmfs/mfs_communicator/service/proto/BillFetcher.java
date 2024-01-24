package com.grameenphone.wipro.fmfs.mfs_communicator.service.proto;

import java.util.Map;

import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.DueBillsResponse.DueBills;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.GetUnpaidBillDetailResponse.BillDetail;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.post_paid_due_bills.DueBillResponse;

public interface BillFetcher<T> {
    default void validateConsumerId(String consumerId, Map map) throws ValidationException {};

    default DueBills convertToGeneric(T t, String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map map) {
        if(t instanceof DueBills) {
            return (DueBills) t;
        }
        return null;
    }

    default DueBills fetchDueBills(DueBillsRequest request) throws HttpErrorResponseException {
        return fetchDueBills(request.getConsumerId(), request.msisdn, request.wallet_type, request.channel, request.params);
    }

    default T fetchDueBills(String consumerId, Map map) {return null;} //It has default implement to give provision for implementors to either override this one or other one
    
    default DueBills fetchDueBills(String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map map) throws HttpErrorResponseException {
        validateConsumerId(consumerId, map);
        T t = fetchDueBills(consumerId, map);
        return convertToGeneric(t, consumerId, msisdn, wallet_type, channel, map);
    }
    
    default BillDetail getBillInfo(String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map map) throws ValidationException
    {
		return null;
	}

    // New Implementation for getting due bills:
    default DueBillResponse fetchDueBillList(DueBillsRequest request) throws  ValidationException {
        return fetchDueBillList(request.getCompany(), request.getConsumerId(), request.msisdn, request.wallet_type, request.channel, request.params);
    }

    default DueBillResponse fetchDueBillList(String company, String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map map) throws ValidationException {
        validateConsumerId(consumerId, map);
        T t = fetchDueBills(consumerId, map);
        return convertToGenerics(t, consumerId, msisdn, wallet_type, channel, map);
    }

    default DueBillResponse convertToGenerics(T t, String consumerId, String msisdn, WalletType wallet_type, Channel channel, Map map) {
        if(t instanceof DueBillResponse) {
            return (DueBillResponse) t;
        }
        return null;
    }
    
}