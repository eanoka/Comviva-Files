package com.grameenphone.wipro.fmfs.cbp.service;

import com.grameenphone.wipro.constants.Events;
import com.grameenphone.wipro.fmfs.cbp.Application;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Action;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.PaymentRequestHop;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.PaymentRequestHopRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.UserRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.WorkflowRepository;
import com.grameenphone.wipro.utility.common.Event;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.apache.commons.collections.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowService {
    @Autowired
    WorkflowRepository workflowRepository;

    @Autowired
    PaymentRequestService paymentRequestService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PaymentRequestHopRepository paymentRequestHopRepository;

    @Autowired
    @Lazy
    WorkflowService selfService;

    public WorkflowService getLazyService() {
        return Application.context.getBean(WorkflowService.class);
    }

    @PostConstruct
    public void handleEvents() {
        Event.async(Events.HOP_CONFIG_CHANGE, (id) -> selfService.updateWorkflowExecutors((Long)id));

        Event.async(Events.ROLE_PERMISSIONS_CHANGE, (r) -> selfService.updateWorkflowExecutors(null, (Role)r));

        Event.async(Events.USER_CREATE, (u) -> selfService.updateWorkflowExecutors((User) u, ((User) u).getRole()));

        Event.async(Events.USER_PERMISSIONS_CHANGE, (p) -> {
            User u;
            Role r = null;
            if (p instanceof User) {
                u = (User) p;
            } else {
                Object[] ps = (Object[]) p;
                u = (User) ps[0];
                r = (Role) ps[1];
            }
            selfService.updateWorkflowExecutors(u, r);
        });
    }

    private void updateWorkflowExecutors(long clientId, Set<Action> actions) {
        List<PaymentRequestHop> requestHops = paymentRequestService.getAwaitingPaymentRequestHops(clientId, actions);
        requestHops.forEach(hop -> {
            List<User> users = userRepository.getCustomLevelApprovers(hop.getPaymentRequest(), hop.getWorkflowHop().getRequiredAction().getName());
            hop.setPossibleExecutors(users);
            paymentRequestHopRepository.save(hop);
        });
    }

    @Transactional
    public void updateWorkflowExecutors(Long clientId) {
        List<Action> manualActions = workflowRepository.getManualExecutableActions(clientId);
        updateWorkflowExecutors(clientId, manualActions.stream().collect(Collectors.toSet()));
    }

    @Transactional
    public void updateWorkflowExecutors(User user, Role oldRole) {
        long clientId;
        if(user != null) {
            clientId = user.getClient().getId();
        } else if(oldRole != null) {
            clientId = oldRole.getClient().getId();
        } else {
            return;
        }

        List<Role> considerableRoles = new ArrayList<>();
        List<User> considerableUsers = new ArrayList<>();
        if(user == null) {
            considerableRoles.add(oldRole); //Role Permission Change
        } else {
            if(oldRole == null) {
                considerableUsers.add(user); //User Permission Change
                considerableRoles.add(user.getRole());
            } else {
                considerableRoles.add(oldRole); //Role change or create
                if(oldRole != user.getRole()) {
                    considerableRoles.add(user.getRole()); //User Create
                }
            }
        }

        List<Action> manualActions = workflowRepository.getManualExecutableActions(clientId);
        Set<Action> applicableActions = new LinkedHashSet<>();
        List<Action> nonApplicableActions = new ArrayList<>(manualActions);

        considerableUsers.forEach(u -> {
            if(nonApplicableActions.isEmpty()) {
                return;
            }
            List<Action> userActions = u.getActions().stream().map(ua -> ua.getAction()).collect(Collectors.toList());
            List<Action> matchedActions = ListUtils.intersection(userActions, nonApplicableActions);
            applicableActions.addAll(matchedActions);
            nonApplicableActions.removeAll(matchedActions);
        });
        considerableRoles.forEach(r -> {
            if(nonApplicableActions.isEmpty()) {
                return;
            }
            List<Action> roleActions = r.getActions().stream().map(ra -> ra.getAction()).collect(Collectors.toList());
            if(r.getInheritedFrom() != null) {
                roleActions.addAll(r.getInheritedFrom().getActions().stream().map(ra -> ra.getAction()).collect(Collectors.toList()));
            }
            List<Action> matchedActions = ListUtils.intersection(roleActions, nonApplicableActions);
            applicableActions.addAll(matchedActions);
            nonApplicableActions.removeAll(matchedActions);
        });

        if (!applicableActions.isEmpty()) {
            updateWorkflowExecutors(clientId, applicableActions);
        }
    }
}