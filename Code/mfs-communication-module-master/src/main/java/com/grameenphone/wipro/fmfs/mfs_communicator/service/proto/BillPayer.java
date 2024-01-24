package com.grameenphone.wipro.fmfs.mfs_communicator.service.proto;

import com.grameenphone.wipro.enums.BillPayStatus;
import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.EventType;
import com.grameenphone.wipro.enums.WalletType;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.exception.TaggedCheckedException;
import com.grameenphone.wipro.exception.ValidationException;
import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import com.grameenphone.wipro.fmfs.mfs_communicator.event.BillPayEvent;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.ServiceChargePaidAmount;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.mfs_payload.MfsResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.BillPayServiceStatus;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.Company;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.BillPayServiceStatusRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.CompanyRepository;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.MFSService;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.spring.ContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Interface to define all possible steps in payment flow
 *
 * @param <A> Type of state object
 * @param <B> Type of company payment API response object
 */
public interface BillPayer<A extends PaymentState, B> {
    Logger logger = LoggerFactory.getLogger(BillPayer.class);
    MFSService mfsService = ContextUtil.getBean(MFSService.class);
    BillPayServiceStatusRepository billPayServiceStatusRepository = ContextUtil.getBean(BillPayServiceStatusRepository.class);
    CompanyRepository companyRepository = ContextUtil.getBean(CompanyRepository.class);
    ApplicationEventPublisher appEventPublisher = Application.context;

    /**
     * To store state of different steps of payment flow execution
     *
     * @return
     */
    default A getState() {
        return (A) new PaymentState();
    }

    String getCategory();

    /**
     * Necessary request parameter validation.
     *
     * @param a
     * @param request
     * @throws ValidationException If there be a validation error
     */
    default void validateRequest(A a, PaymentRequest request) throws ValidationException {
    }

    /**
     * This function is called after validation and before wallet deduction
     *
     * @param a
     * @param request
     * @throws HttpErrorResponseException
     */
    default void preparePayment(A a, PaymentRequest request) throws HttpErrorResponseException {
    }

    /**
     * Checks payer's association in MFS
     *
     * @param a
     * @param request
     * @return
     */
    default boolean checkAssociation(A a, PaymentRequest request) {
        return mfsService.isCustomerAssociated(request.wallet_type == WalletType.RET ? request.customer : request.msisdn, request.getConsumerId(), request.getMFSCompany());
    }

    /**
     * Associates in MFS if not have already
     *
     * @param a
     * @param request
     * @param channel
     * @throws IOException
     */
    default void associate(A a, PaymentRequest request, String channel) throws IOException {
        if (request.wallet_type == WalletType.RET) {
            mfsService.associateRetailer(a.sessionId, request.msisdn, request.customer, request.getConsumerId(), request.getMFSCompany(), getCategory(), channel);
        } else {
            mfsService.associateSubscriber(a.sessionId, request.msisdn, request.getConsumerId(), request.getMFSCompany(), getCategory(), channel);
        }
    }

    /**
     * Invokes after wallet deduction to perform any third party notification
     *
     * @param a
     * @param request
     * @param mfsChannel
     * @return
     * @throws IOException
     */
    B pay(A a, PaymentRequest request, String mfsChannel) throws IOException;

    /**
     * Generates final pay api response from company specific state object
     *
     * @param a
     * @param b
     * @param request
     * @return
     */
    default PaymentResult convertToGeneric(A a, B b, PaymentRequest request) {
        if (b instanceof PaymentResult) {
            return (PaymentResult) b;
        }
        return null;
    }

    /**
     * Generate final pay api response from exception occurred in different steps
     *
     * @param t
     * @return
     */
    default PaymentResult handleException(Throwable t) {
        logger.error("Unable to pay for exception");
        throw new TaggedCheckedException(t);
    }
    
    default String getComvivaBillNumber(PaymentRequest paymentRequest) {
        return paymentRequest.bill;
    }

    default BillPayServiceStatus prepareBillPayModel(String accountNo, double amount, String billNo, String categoryCode, String companyCode, String mfsTxnid, String mfsTxnStatus, String thirdPartyTxnid, String thirdPartyTxnStatus, String payerMsisdn, String customerMsisdn, String sessionId, String walletType, String channel, String creator, Double paidAmount, Double serviceCharge) {
        BillPayServiceStatus billPayServiceStatus = new BillPayServiceStatus();
        billPayServiceStatus.setAccountNo(accountNo);
        billPayServiceStatus.setAmount(amount);
        billPayServiceStatus.setBillNo(billNo);
        billPayServiceStatus.setCategoryCode(categoryCode);
        billPayServiceStatus.setCompanyCode(companyCode);
        billPayServiceStatus.setCreatedBy(creator == null ? "generic payment layer" : creator);
        billPayServiceStatus.setLastUpdatedBy("generic payment layer");
        billPayServiceStatus.setMfsTxnid(mfsTxnid);
        billPayServiceStatus.setMfsTxnStatus(mfsTxnStatus);
        billPayServiceStatus.setMsisdn(payerMsisdn);
        billPayServiceStatus.setCustomerMsisdn(customerMsisdn);
        billPayServiceStatus.setSessionId(sessionId);
        billPayServiceStatus.setStatus(BillPayStatus.FAIL);
        billPayServiceStatus.setTransactionType(walletType + "BILLPAY");
        billPayServiceStatus.setChannel(channel);
        billPayServiceStatus.setPaidAmount(paidAmount);
        billPayServiceStatus.setServiceCharge(serviceCharge);
        billPayServiceStatus.setCreationDate(new Timestamp(System.currentTimeMillis()));
        billPayServiceStatus.setThirdPartyTxnid(thirdPartyTxnid);
        billPayServiceStatus.setThirdPartyTxnStatus(thirdPartyTxnStatus);
        return billPayServiceStatus;
    }

    @Transactional()
    default PaymentResult payBill_Old_API(PaymentRequest request) {
        PaymentResult result = null;
        A state = getState();
        String mfsChannel = mfsService.getMfsChannel(request.channel);
        try {
            validateRequest(state, request);
            preparePayment(state, request);
            try {
                if (!checkAssociation(state, request)) {
                    logger.debug("Association Not Found");
                    associate(state, request, mfsChannel);
                } else {
                    logger.debug("Association Found for Company:" + request.getCompany() + " Account:" + request.getConsumerId() + " MSISDN: " + (StringUtil.isNullOrEmpty(request.customer) ? request.msisdn : request.customer));
                }
            } catch (Throwable h) {
                logger.error("Association Failed", h);
            }
            Company company = companyRepository.findCompanyByCompanyCode(request.getCompany());
            Integer surcharge = null;
            if (company.hasSurcharge) {
                Map<String, Object> params = request.params;
                if (params != null && (surcharge = (Integer) params.get("surcharge")) != null) {
                } else {
                    surcharge = 0;
                }
            }
            MfsResponse response = state.mfsPaymentResponse = mfsService.deductWallet(state.sessionId, request.msisdn, request.pin, request.getConsumerId(), getComvivaBillNumber(request), request.amount, surcharge, request.getCompany(), request.getMFSCompany(), getCategory(), mfsChannel, request.wallet_type == WalletType.RET);
            if (StringUtil.hasText(response.txnid)) {
                ServiceChargePaidAmount serviceChargePaidAmount = mfsService.getServiceChargeAndPaidAmount(response.txnid);
             //   state.billPayServiceStatus = billPayServiceStatusRepository.save(prepareBillPayModel(request.getConsumerId(), request.amount, request.bill, getCategory(), request.getCompany(), response.txnid, response.txnstatus, request.msisdn, request.customer, state.sessionId, request.wallet_type.name(), request.channel.name(), request.initiator, serviceChargePaidAmount.paidAmount, serviceChargePaidAmount.serviceCharge));
                state.serviceCharge = serviceChargePaidAmount.serviceCharge;
                state.paidAmount = serviceChargePaidAmount.paidAmount;
            }
            if (!response.txnstatus.equals("200")) {
                result = new PaymentResult();
                result.status = BillPayStatus.FAIL;
                result.message = response.message;
                result.failCode = response.txnstatus;
                return result;
            }
            B paymentResponse = pay(state, request, mfsChannel);
            result = convertToGeneric(state, paymentResponse, request);
            if(result.paidAmount != state.paidAmount) {
                result.paidAmount = state.paidAmount;
                result.serviceCharge = state.serviceCharge;
            }
            return result; 
        } catch (Throwable t) {
            return handleException(t);
        } finally {
            if (result != null && result.status.equals("Success")) {
                BillPayEvent billPayEvent = new BillPayEvent();
                BillPayEvent.Data data = new BillPayEvent.Data();

                data.amount = request.amount;
                billPayEvent.channel = state.billPayServiceStatus.getChannel();
                if(billPayEvent.channel.equals("USSD")) {
                    billPayEvent.channel += "_" + state.billPayServiceStatus.getTransactionType().substring(0, 3);
                }
                data.service_charge = state.billPayServiceStatus.getServiceCharge();
                data.utility = request.getCompany();

                billPayEvent.data = data;
                billPayEvent.initiator = request.msisdn;
                billPayEvent.initiator_on_behalf = request.customer;
                data.mfs_txn_id = state.billPayServiceStatus.getMfsTxnid();

                appEventPublisher.publishEvent(billPayEvent);
            }
        }
    }


    @Transactional()
    default PaymentResult payBill(PaymentRequest request) {
        PaymentResult result = null;
        A state = getState();
        String mfsChannel = mfsService.getMfsChannel(request.channel);
        try {
            validateRequest(state, request);
            preparePayment(state, request);
            B paymentResponse = pay(state, request, mfsChannel);
            result = convertToGeneric(state, paymentResponse, request);
            if(result.paidAmount != state.paidAmount) {
                result.paidAmount = state.paidAmount;
                result.serviceCharge = state.serviceCharge;
            }
            return result;
        } catch (Throwable t) {
            return handleException(t);
        } finally {
            if (result != null && result.status.equals("Success")) {
                BillPayEvent billPayEvent = new BillPayEvent();
                BillPayEvent.Data data = new BillPayEvent.Data();

                data.amount = request.amount;
                billPayEvent.channel = state.billPayServiceStatus.getChannel();
                if(billPayEvent.channel.equals("USSD")) {
                    billPayEvent.channel += "_" + state.billPayServiceStatus.getTransactionType().substring(0, 3);
                }
                data.service_charge = state.billPayServiceStatus.getServiceCharge();
                data.utility = request.getCompany();
                data.mfs_txn_id = result.txnId;
                billPayEvent.data = data;
                billPayEvent.initiator = request.msisdn;
                billPayEvent.initiator_on_behalf = request.customer;
                data.mfs_txn_id = state.billPayServiceStatus.getMfsTxnid();
                appEventPublisher.publishEvent(billPayEvent);
            }
        }
    }
}