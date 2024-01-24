package com.grameenphone.wipro.fmfs.cbp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.grameenphone.wipro.constants.EventScopes;
import com.grameenphone.wipro.constants.Events;
import com.grameenphone.wipro.fmfs.cbp.enums.WorkflowHops;
import com.grameenphone.wipro.fmfs.cbp.model.data.saml.Response;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.data.websocket.SocketMessage;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Action;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.RoleAction;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.UserAction;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.WorkflowHop;
import com.grameenphone.wipro.fmfs.cbp.model.view.payment_request.BillPaymentApprovalResponse;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao.ClosableCrudDao;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.ActionRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.RoleRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.UserRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.WorkflowRepository;
import com.grameenphone.wipro.utility.common.Event;
import com.grameenphone.wipro.utility.marshal.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    private final static Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    @Lazy
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    ActionRepository actionRepository;

    @Autowired
    NotificationService notificationService;

    @Autowired
    WorkflowRepository workflowRepository;

    @Transactional
    public void authenticate(Response samlResponse) {
        User authenticated = userService.findUser(samlResponse.Assertion.AttributeStatement.stream().filter(s -> s.Name.equals("username")).map(s -> s.AttributeValue).findFirst().get());
        if(authenticated == null) {
            try {
                logger.debug("No user found for the response: " + Xml.toXml(samlResponse));
            } catch (JsonProcessingException e) {
            }
            throw new UsernameNotFoundException("No user found for underlying response");
        }
        Client client = authenticated.getClient();
        if(client != null) {
            if(!client.isActive()) {
                throw new UsernameNotFoundException("Underlying client for this user is not active");
            }
            if(userService.getActiveAdminCount(client) == 0) {
                throw new UsernameNotFoundException("No active admin found in underlying client");
            }
        }
        SessionAttributes.current(true).setUser(authenticated, userService.getPermittedActions(authenticated), hasGpRole(authenticated), samlResponse);

        String frontend_unique = SessionAttributes.current().FRONTEND_UNIQUE;
        SessionObject finalSessionObjectForEventHandler = SessionAttributes.current();
        Runnable runnable = () -> {
            ArrayList<String> permissions = new ArrayList<>();
            //Session object cannot be refreshed as before commit updated data can't be found from other thread
            Event.one(Events.ENTITY_MANAGER_CLOSE, EventScopes.REQUEST + "-" + Thread.currentThread().getId(), (d) -> {
                if("cbp".equals(d)) {
                    try {
                        Thread thread = new Thread(() -> {
                            try (ClosableCrudDao<User> userDao = CrudDao.getInNewSession(User.class)) {
                                User userOfCurrentSession = userDao.findOne(authenticated.getId());
                                permissions.addAll(userService.getPermittedActions(userOfCurrentSession));
                            }
                        });
                        thread.start();
                        thread.join();
                        finalSessionObjectForEventHandler.setPermissions(permissions);
                        notificationService.notifyUser(frontend_unique, new SocketMessage().setTopic("access_permissions").setMessage("modified"));
                    } catch (Exception f) {
                        logger.error("Could not send permission change event");
                    }
                }
            });
        };
        Event.on(Events.USER_PERMISSIONS_CHANGE, Arrays.asList(EventScopes.SESSION, "" + authenticated.getId()), runnable);
        Event.on(Events.ROLE_PERMISSIONS_CHANGE, Arrays.asList(EventScopes.SESSION, "" + authenticated.getRole().getId()), runnable);
        if(authenticated.getRole().getInheritedFrom() != null) {
            Event.on(Events.ROLE_PERMISSIONS_CHANGE, Arrays.asList(EventScopes.SESSION, "" + authenticated.getRole().getInheritedFrom().getId()), runnable);
        }

        samlResponse.Assertion.AttributeStatement.stream().filter(s -> s.Name.equals("display_name")).map(s -> s.AttributeValue).findFirst().ifPresent(fullName -> {
            if(!fullName.equals(authenticated.getName())) {
                authenticated.setName(fullName);
                userRepository.save(authenticated);
            }
        });
        authenticated.setLastLoginTime(new Date());
        logger.debug(authenticated.getName() + " Logged");
    }

    public boolean hasGpRole(User user) {
        Role role = user.getRole();
        if(!role.isReadonly()) {
            role = role.getInheritedFrom();
        }
        return role.isForGp();
    }

    public void logout() {
        User user = SessionAttributes.current().getUser();
        if(user != null) {
            SessionAttributes.current().unsetUser();
            logger.debug(user.getName() + " Logged Out");
        }
    }

    public boolean isPermitted(String action) {
        return SessionAttributes.current().getPermissions().contains(action);
    }

    public List<Action> getCustomActions() {
        if(SessionAttributes.current().IS_GP) {
            return new ArrayList<>();
        }
        return actionRepository.findByClientId(SessionAttributes.current().getUser().getClient().getId());
    }

    public List<BillPaymentApprovalResponse.BillPaymentApprovalHop> getCustomWorkflowHops() {
        if(SessionAttributes.current().IS_GP) {
            return new ArrayList<>();
        }
        List<BillPaymentApprovalResponse.BillPaymentApprovalHop> allApprovalHops = new ArrayList<>();
        List<WorkflowHop> hops = workflowRepository.findByCodeAndClientIdOrderByOrder(WorkflowHops.WFA, SessionAttributes.current().getUser().getClient().getId());
        for(WorkflowHop hop : hops) {
            BillPaymentApprovalResponse.BillPaymentApprovalHop approvalHop = new BillPaymentApprovalResponse.BillPaymentApprovalHop();
            approvalHop.hop = hop;
            approvalHop.users = getPermittedUsersForAction(hop.getRequiredAction().getName());
            approvalHop.roles = getPermittedRolesForAction(hop.getRequiredAction().getName());
            allApprovalHops.add(approvalHop);
        }
        return allApprovalHops;
    }

    public List<User> getPermittedUsersForAction(String action) {
        SessionObject session = SessionAttributes.current();
        User currentUser = session.getUser();
        Client client = currentUser.getClient();
        return CrudDao.get(User.class).query()
                .eq("deleted", false)
                .eqif(() -> session.IS_GP || currentUser.isAllowAllDivision(), "client", client)
                .mrif(() -> !session.IS_GP && !currentUser.isAllowAllDivision(), "clientDivisions", () -> currentUser.getClientDivisions())
                .eqif(() -> action != null, "actions.action.name", action)
                .eqif(() -> action != null, "actions.allowed", true).findAll();
    }

    public List<Role> getPermittedRolesForAction(String action) {
        SessionObject session = SessionAttributes.current();
        User currentUser = session.getUser();
        Client client = currentUser.getClient();
        return CrudDao.get(Role.class).query()
                .eqif(() -> action != null, "actions.action.name", action)
                .eqif(() -> action != null, "actions.allowed", true)
                .or()
                    .eq("client", client)
                    .eq("isForGp", session.IS_GP)
                .close().findAll();
    }

    @Transactional
    public void allowUserNRolesForAction(List<Long> usersToAllow, List<Long> rolesToAllow, String action) {
        Action actionEntity = actionRepository.findByName(action);
        List<User> currentPermittedUsers = getPermittedUsersForAction(action);
        List<Role> currentPermittedRoles = getPermittedRolesForAction(action);

        CrudDao<UserAction> userActionCrudDao = CrudDao.get(UserAction.class);
        CrudDao<RoleAction> roleActionCrudDao = CrudDao.get(RoleAction.class);
        for(User u : currentPermittedUsers) {
            if(!usersToAllow.contains(u.getId())) {
                userActionCrudDao.delete(u.getActions().stream().filter(ua -> ua.getAction() == actionEntity).findFirst().get());
            } else {
                usersToAllow.remove(u.getId());
            }
        }
        for(Role r : currentPermittedRoles) {
            if(!rolesToAllow.contains(r.getId())) {
                roleActionCrudDao.delete(r.getActions().stream().filter(ua -> ua.getAction() == actionEntity).findFirst().get());
            } else {
                rolesToAllow.remove(r.getId());
            }
        }

        for(Long u : usersToAllow) {
            User user = userRepository.findById(u).get();
            Optional<UserAction> optionalUa = user.getActions().stream().filter(ua -> ua.isDenied()).findFirst();
            if(optionalUa.isPresent()) {
                userActionCrudDao.delete(optionalUa.get());
            } else {
                UserAction ua = new UserAction();
                ua.setAction(actionEntity);
                ua.setUser(user);
                ua.setAllowed(true);
                ua.setDenied(false);
                userActionCrudDao.save(ua);
            }
        }
        for(Long r : rolesToAllow) {
            Role role = roleRepository.findById(r).get();
            Optional<RoleAction> optionalUa = role.getActions().stream().filter(ua -> ua.isDenied()).findFirst();
            if(optionalUa.isPresent()) {
                roleActionCrudDao.delete(optionalUa.get());
            } else {
                RoleAction ua = new RoleAction();
                ua.setAction(actionEntity);
                ua.setRole(role);
                ua.setAllowed(true);
                ua.setDenied(false);
                roleActionCrudDao.save(ua);
            }
        }
    }
}