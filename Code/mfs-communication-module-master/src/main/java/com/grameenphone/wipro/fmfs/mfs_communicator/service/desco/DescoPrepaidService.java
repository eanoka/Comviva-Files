package com.grameenphone.wipro.fmfs.mfs_communicator.service.desco;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.HttpResponse;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload.PaymentResponse.PaymentResult;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.state.PaymentState;
import com.grameenphone.wipro.fmfs.mfs_communicator.service.proto.BillPayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class DescoPrepaidService implements BillPayer<PaymentState, Void> {

    @Autowired
    @Qualifier("DSCOP_Kaifa_Bill_Payer")
    DescoSmartService descoSmartService;

    @Autowired
    @Qualifier("DSCOP_Unified_Bill_Payer")
    DescoUnifiedMeterService descoUnifiedMeterService;

    @Bean({"DSCOP_Bill_Payer"})
    public DescoPrepaidService alias() {
        return this;
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public Void pay(PaymentState state, PaymentRequest request, String mfsChannel) throws IOException {
        return null;
    }

    @Override
    public PaymentResult convertToGeneric(PaymentState state, Void aVoid, PaymentRequest request) {
        return null;
    }

    @Override
    public PaymentResult payBill(PaymentRequest request) {
        if (request.params.containsKey("unified_meter") && (Boolean) request.params.get("unified_meter")) {
            return descoUnifiedMeterService.payBill(request);
        } else {
            PaymentResult result = new PaymentResult();
            HttpResponse webServiceResponse = descoSmartService.topUpBreakDown(request.getConsumerId(), request.amount);
            if (webServiceResponse.isError) {
                if ("404".equals(webServiceResponse.responseCode)) {
                    return descoUnifiedMeterService.payBill(request);
                } else {
                    result.status = "400";
                    result.message = webServiceResponse.response;
                    return result;
                }
            }
            request.params.put("transaction_id", webServiceResponse.response);
            return descoSmartService.payBill(request);
        }
    }
}