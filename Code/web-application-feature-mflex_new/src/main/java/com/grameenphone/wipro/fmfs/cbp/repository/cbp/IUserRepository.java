package com.grameenphone.wipro.fmfs.cbp.repository.cbp;

import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.ClientDivision;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.PaymentRequest;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;

import java.util.Collection;
import java.util.List;

public interface IUserRepository {
    List<User> getBilldataValidators(Collection<ClientDivision> divisions);

    List<User> getFirstLevelApprovers(PaymentRequest request);

    List<User> getCustomLevelApprovers(PaymentRequest request, String actionName);

    List<User> getPaymentFinalizers(PaymentRequest request);
}