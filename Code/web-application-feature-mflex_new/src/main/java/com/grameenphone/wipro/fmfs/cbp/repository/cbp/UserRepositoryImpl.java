package com.grameenphone.wipro.fmfs.cbp.repository.cbp;

import com.grameenphone.wipro.fmfs.cbp.consts.Actions;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserRepositoryImpl implements IUserRepository {
    @PersistenceContext
    EntityManager entityManager;

    public List<User> getBilldataValidators(Collection<ClientDivision> divisions) {
        return getUserWithPermission(divisions, Actions.VALIDATE_BILL_DATA);
    }

    public List<User> getFirstLevelApprovers(PaymentRequest request) {
        return getUserWithPermission(request.getClientDivisions(), Actions.APPROVE_PAYMENT);
    }

    public List<User> getCustomLevelApprovers(PaymentRequest request, String actionName) {
        return getUserWithPermission(request.getClientDivisions(), actionName);
    }

    /**
     * List all users that have access to all of the given divisions and is permitted for the given action.
     * This function must be called for client users and actions
     * @param divisions
     * @param action
     * @return
     */
    private List<User> getUserWithPermission(Collection<ClientDivision> divisions, String action) {
        if(divisions.isEmpty()) {
            return new ArrayList<>();
        }
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> pr = cq.from(User.class);

        Subquery divisionSq = cq.subquery(Long.class);
        Root<User> ur = divisionSq.from(User.class);
        divisionSq.where(cb.equal(pr.get("id"), ur.get("id")), ur.join("clientDivisions").in(divisions));
        divisionSq.select(cb.count(ur));

        //Approve not denied user level
        Subquery<Long> sq1 = cq.subquery(Long.class);
        Root<UserAction> sr = sq1.from(UserAction.class);
        sq1.where(cb.equal(sr.get("action").get("name"), action), cb.equal(sr.get("denied"), true), cb.equal(pr.get("id"), sr.get("user").get("id")));
        sq1.select(cb.count(sr));

        //Approve is allowed user level
        Subquery<Long> sq3 = cq.subquery(Long.class);
        Root<UserAction> sra = sq3.from(UserAction.class);
        sq3.where(cb.equal(sra.get("action").get("name"), action), cb.equal(sra.get("allowed"), true), cb.equal(pr.get("id"), sra.get("user").get("id")));
        sq3.select(cb.count(sra));

        //Approve not denied role level
        Subquery<Long> sq2 = cq.subquery(Long.class);
        Root<RoleAction> rr = sq2.from(RoleAction.class);
        Path<Role> prr = pr.get("role");
        Path<Role> rrr = rr.get("role");
        sq2.where(cb.equal(rr.get("action").get("name"), action), cb.equal(rr.get("denied"), true), cb.or(cb.equal(prr.get("id"), rrr.get("id")), cb.equal(prr.get("inheritedFrom").get("id"), rrr.get("id"))));
        sq2.select(cb.count(rr));

        //Approve is allowed role level
        Subquery<Long> sq4 = cq.subquery(Long.class);
        Root<RoleAction> rra = sq4.from(RoleAction.class);
        Path<Role> prra = pr.get("role");
        Path<Role> rrra = rra.get("role");
        sq4.where(cb.equal(rra.get("action").get("name"), action), cb.equal(rra.get("allowed"), true), cb.or(cb.equal(prra.get("id"), rrra.get("id")), cb.equal(prra.get("inheritedFrom").get("id"), rrra.get("id"))));
        sq4.select(cb.count(rra));

        cq.where(cb.equal(pr.get("active"), true), cb.equal(pr.get("deleted"), false), cb.or(cb.and(cb.equal(pr.get("allowAllDivision"), true), cb.equal(pr.get("client"), divisions.iterator().next().getClient())), cb.and(cb.equal(pr.get("allowAllDivision"), false), cb.equal(divisionSq, divisions.size()))), cb.equal(sq1, 0), cb.equal(sq2, 0), cb.or(cb.gt(sq3, 0), cb.gt(sq4, 0)));

        cq.select(pr);
        return entityManager.createQuery(cq).getResultList();
    }

    public List<User> getPaymentFinalizers(PaymentRequest request) {
        return getUserWithPermission(request.getClientDivisions(), Actions.INITIATE_PAYMENT);
    }
}