package com.grameenphone.wipro.fmfs.cbp.service;

import com.grameenphone.wipro.constants.Events;
import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.fmfs.cbp.consts.Actions;
import com.grameenphone.wipro.fmfs.cbp.consts.SystemRoles;
import com.grameenphone.wipro.fmfs.cbp.consts.UserActivities;
import com.grameenphone.wipro.fmfs.cbp.enums.WorkflowHops;
import com.grameenphone.wipro.fmfs.cbp.model.data.idp.Response;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.*;
import com.grameenphone.wipro.fmfs.cbp.model.view.role.AllowDeny;
import com.grameenphone.wipro.fmfs.cbp.model.view.role.UpdatePermissionRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.user.CreateFormInitializationDataResponse;
import com.grameenphone.wipro.fmfs.cbp.model.view.user.PaginatedUser;
import com.grameenphone.wipro.fmfs.cbp.model.view.user.UserCreationRequest;
import com.grameenphone.wipro.fmfs.cbp.model.view.user.UserExcelReport;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.UserActivityCommentRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.UserRepository;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.WorkflowRepository;
import com.grameenphone.wipro.utility.common.Event;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.common.HttpClient.HttpMethod;
import com.grameenphone.wipro.utility.common.StringUtil;
import com.grameenphone.wipro.utility.excel.ExcelWriter;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.orm.WhereBuilder;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final static Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    BillDataService billDataService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    WorkflowRepository workflowRepository;

    @Autowired
    UserActivityCommentRepository userActivityCommentRepository;

    @Value("${idp.user.url.base}")
    String idpUrlBase;

    @Value("${idp.api.key}")
    String idpApiKey;

    @Value("${idp.is.proxy.required}")
    boolean idpIsProxyRequired;

    /**
     * Determines all permitted actions for a user.
     * @param user
     * @return
     */
    public List<String> getPermittedActions(User user) {
        Set<String> permittedActions = new LinkedHashSet<>();
        Collection<UserAction> userActionRelations = user.getActions();
        BiFunction<Collection<UserAction>, Boolean, Collection<String>> userActionExtractor = (uas, forAllowed) -> uas.stream().filter(forAllowed ? UserAction::isAllowed : UserAction::isDenied).map(ua -> ua.getAction().getName()).collect(Collectors.toSet());
        Collection<String> allowedActionsFromUser = userActionExtractor.apply(userActionRelations, true);
        Collection<String> deniedActionsFromUser = userActionExtractor.apply(userActionRelations, false);
        Role role = user.getRole();
        Collection<RoleAction> roleActionRelations = role.getActions();
        BiFunction<Collection<RoleAction>, Boolean, Collection<String>> roleActionExtractor = (uas, forAllowed) -> uas.stream().filter(forAllowed ? RoleAction::isAllowed : RoleAction::isDenied).map(ua -> ua.getAction().getName()).collect(Collectors.toSet());
        Collection<String> allowedActionsFromRole = roleActionExtractor.apply(roleActionRelations, true);
        Collection<String> deniedActionsFromRole = roleActionExtractor.apply(roleActionRelations, false);
        role = role.getInheritedFrom();
        if (role != null) {
            roleActionRelations = role.getActions();
            allowedActionsFromRole.addAll(roleActionExtractor.apply(roleActionRelations, true));
            deniedActionsFromRole.addAll(roleActionExtractor.apply(roleActionRelations, false));
        }
        permittedActions.addAll(allowedActionsFromUser);
        permittedActions.addAll(allowedActionsFromRole);
        permittedActions.removeAll(deniedActionsFromUser);
        permittedActions.removeAll(deniedActionsFromRole);

        if(user.getClient() != null) {
            List<String> paymentApprovalActions = workflowRepository.getAllPaymentApprovalHops(WorkflowHops.WFA, user.getClient().getId()).stream().map(h -> h.getRequiredAction().getName()).collect(Collectors.toList());
            if(paymentApprovalActions.stream().anyMatch(p -> permittedActions.contains(p))) {
                permittedActions.add("$APPROVE_PAYMENT_ANY_LEVEL$");
            }
        }
        return new ArrayList<>(permittedActions);
    }

    public User getUser(long id) {
        return CrudDao.get(User.class).findOne(id);
    }

    public User findUser(String loginId) {
        return CrudDao.get(User.class).query().fetch("client", JoinType.LEFT).fetch("clientDivisions", JoinType.LEFT).eq("loginId", loginId).eq("active", true).eq("deleted", false).findOne();
    }

    public List<User> permissionModifiableUsers() {
        return getQueryForUserWithPermittedNonOwnerAction(null).findAll();
    }

    public List<User> permittedUsersFor(String action) {
        return getQueryForUserWithPermittedNonOwnerAction(action).findAll();
    }

    private WhereBuilder<User, ?> getQueryForUserWithPermittedNonOwnerAction(String actionName) {
        SessionObject session = SessionAttributes.current();
        User currentUser = session.getUser();
        Client client = currentUser.getClient();
        return CrudDao.get(User.class).query().eq("deleted", false)
                .or()
                    .and()
                        .eqif(() -> session.IS_GP || currentUser.isAllowAllDivision(), "client", client)
                        .mrif(() -> !session.IS_GP && !currentUser.isAllowAllDivision(), "clientDivisions", () -> currentUser.getClientDivisions())
                    .close()
                    .eqif(() -> actionName != null && session.getPermissions().contains(actionName), "role.name", SystemRoles.CLIENT_ADMIN)
                    .close();
    }

    public PaginatedUser listableUsers(long offset, int perPage) {
        WhereBuilder<User, ?> userQuery = getQueryForUserWithPermittedNonOwnerAction(Actions.LIST_OTHER_ACCOUNT_USERS);
        long count = userQuery.count();
        if(count <= offset) {
            offset = (long)(Math.ceil(count / (double)perPage) - 1) * perPage;
        }
        List<User> records;
        PaginatedUser paginatedUser = new PaginatedUser();
        if(count == 0) {
            offset = 0;
            records = new ArrayList<>();
        } else {
            records = userQuery.findAll(offset, perPage);
        }
        paginatedUser.count = count;
        paginatedUser.offset = offset;
        paginatedUser.perPage = perPage;
        paginatedUser.records = records;
        return paginatedUser;
    }

    public List<AllowDeny> getOwnPermissionsOnly(User user) {
        return user.getActions().stream().map(s -> {
            AllowDeny allowDeny = new AllowDeny();
            allowDeny.name = s.getAction().getName();
            allowDeny.allow = s.isAllowed();
            allowDeny.deny = s.isDenied();
            return allowDeny;
        }).collect(Collectors.toList());
    }

    public List<AllowDeny> getCumulativePermissions(User user) {
        List<AllowDeny> allowDenies = getOwnPermissionsOnly(user);
        Function<Collection<RoleAction>, List<AllowDeny>> c = (roleActions) -> roleActions.stream().map(s -> {
            AllowDeny allowDeny = new AllowDeny();
            allowDeny.name = s.getAction().getName();
            allowDeny.allow = s.isAllowed();
            allowDeny.deny = s.isDenied();
            return allowDeny;
        }).filter(a -> {
            int index = allowDenies.indexOf(a);
            if(index < 0) {
                return true;
            }
            AllowDeny available = allowDenies.get(index);
            if(a.deny) {
                allowDenies.remove(index);
                return true;
            }
            if(available.allow || available.deny) {
                return false;
            }
            allowDenies.remove(index);
            if(a.allow) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());
        Role role = user.getRole();
        allowDenies.addAll(c.apply(role.getActions()));
        if(!role.isReadonly()) {
            allowDenies.addAll(c.apply(role.getInheritedFrom().getActions()));
        }
        return allowDenies;
    }

    @Transactional
    public void updatePermissions(UpdatePermissionRequest request) {
        User user = CrudDao.get(User.class).findOne(request.id);
        if(!permissionModifiableUsers().contains(user)) {
            throw new AppRuntimeException("You are not permitted to modify permission for this user");
        }
        List<Action> allActions = CrudDao.get(Action.class).findAll();
        Collection<UserAction> userActions = user.getActions();
        List<String> applicablePermissions = SessionAttributes.current().getPermissions();
        CrudDao<UserAction> userActionCrudDao = CrudDao.get(UserAction.class);
        request.changes.forEach((x, y) -> {
            if(!applicablePermissions.contains(x)) {
                throw new AppRuntimeException("You are not permitted for one of these permissions to change");
            }
            Optional<UserAction> optionalUserAction = userActions.stream().filter(r -> r.getAction().getName().equals(x)).findFirst();
            UserAction userAction;
            if(optionalUserAction.isEmpty()) {
                userAction = new UserAction();
                int index = allActions.indexOf(new Object() {
                    @Override
                    public boolean equals(Object obj) {
                        return x.equals(((Action)obj).getName());
                    }
                });
                if(index < 0) {
                    throw new AppRuntimeException("Invalid Permission");
                }
                userAction.setUser(user);
                userAction.setAction(allActions.get(index));
                if(y.equals("I")) {
                    return;
                }
                userActionCrudDao.save(userAction);
            } else {
                userAction = optionalUserAction.get();
                if(y.equals("I")) {
                    userActionCrudDao.delete(userAction);
                    return;
                }
                userAction.setAllowed(false);
                userAction.setDenied(false);
            }
            if(y.equals("A")) {
                userAction.setAllowed(true);
            } else if(y.equals("D")) {
                userAction.setDenied(true);
            }
            userActionCrudDao.save(userAction);
        });
        Event.fire(Events.USER_PERMISSIONS_CHANGE, user.getId());
    }

    public long getActiveAdminCount(Client client) {
        return CrudDao.get(User.class).query().eq("active", true).eq("client", client).eqif(() -> client == null, "role.name", SystemRoles.GP_ADMIN).eqif(() -> client != null, "role.name", SystemRoles.CLIENT_ADMIN).count();
    }

    /**
     * return true if barred otherwise false. Throws runtime exception if fails
     * @param id
     * @param remarks
     * @return
     */
    @Transactional
    public boolean toggleActive(Long id, String remarks) {
        User user = getUser(id);
        User loggedUser = SessionAttributes.current().getUser();
        if(loggedUser.equals(user)) {
            throw new AppRuntimeException("You are not allowed to bar yourself", 400);
        }
        if(!permittedUsersFor(Actions.BAR_OTHER_ACCOUNT_USER).contains(user)) {
            throw new AppRuntimeException("You are not authorized to bar/unbar this user", 403);
        }
        Role role = user.getRole();
        if(user.isActive() && role.isReadonly() && (role.isForGp() ? role.getName().equals(SystemRoles.GP_ADMIN) : role.getName().equals(SystemRoles.CLIENT_ADMIN))) {
            long adminUserCount = getActiveAdminCount(user.getClient());
            if(adminUserCount == 1) {
                throw new AppRuntimeException("All the admin can not be barred", 400);
            }
        }
        boolean isBar = user.isActive();
        user.setActive(!isBar);
        userRepository.save(user);
        if(remarks != null) {
            UserActivityComment comment = new UserActivityComment();
            comment.setAction(isBar ? UserActivities.BAR : UserActivities.UNBAR);
            comment.setComment(remarks);
            comment.setPerformedBy(loggedUser);
            comment.setUser(user);
            userActivityCommentRepository.save(comment);
        }
        try {
            HttpClient httpClient = new HttpClient();
            if(idpIsProxyRequired) {
                httpClient.setDefaultProxy();
            }
            Response idpResponse = httpClient.invokeForEntity(HttpMethod.PATCH, idpUrlBase + "/" + user.getIdpUserId(), "sp_user_status=" + (isBar ? "Pending" : "Active"), new HashMap<>(){{
                put("X-Api-Key", idpApiKey);
                put("Content-Type", "application/x-www-form-urlencoded");
            }}, Response.class);
            if(!idpResponse.success) {
                throw new AppRuntimeException("Failed to update user status");
            }
        } catch (IOException e) {
            throw new AppRuntimeException("Failed to update user status");
        }
        return isBar;
    }

    /**
     * return true if barred otherwise false. Throws runtime exception if fails
     * @param id
     * @param remarks
     * @return
     */
    @Transactional
    public void delete(Long id, String remarks) {
        User user = getUser(id);
        if(user.isDeleted()) {
            return;
        }
        User loggedUser = SessionAttributes.current().getUser();
        if(loggedUser.equals(user)) {
            throw new AppRuntimeException("You are not allowed to delete yourself", 400);
        }
        if(!permittedUsersFor(Actions.DELETE_OTHER_ACCOUNT_USER).contains(user)) {
            throw new AppRuntimeException("You are not authorized to delete this user", 403);
        }
        Role role = user.getRole();
        if(user.isActive() && role.isReadonly() && (role.isForGp() ? role.getName().equals(SystemRoles.GP_ADMIN) : role.getName().equals(SystemRoles.CLIENT_ADMIN))) {
            long adminUserCount = getActiveAdminCount(user.getClient());
            if(adminUserCount == 1) {
                throw new AppRuntimeException("All the admin can not be deleted", 400);
            }
        }
        user.setLoginId(System.currentTimeMillis() + "-" + user.getLoginId());
        user.setDeleted(true);
        userRepository.save(user);
        if(remarks != null) {
            UserActivityComment comment = new UserActivityComment();
            comment.setAction(UserActivities.DELETE);
            comment.setComment(remarks);
            comment.setPerformedBy(loggedUser);
            comment.setUser(user);
            userActivityCommentRepository.save(comment);
        }
        try {
            HttpClient httpClient = new HttpClient();
            if(idpIsProxyRequired) {
                httpClient.setDefaultProxy();
            }
            Response idpResponse = httpClient.invokeForEntity(HttpMethod.PATCH, idpUrlBase + "/" + user.getIdpUserId(), "sp_user_status=Removed", new HashMap<>(){{
                put("X-Api-Key", idpApiKey);
                put("Content-Type", "application/x-www-form-urlencoded");
            }}, Response.class);
            if(!idpResponse.success) {
                throw new AppRuntimeException("Failed to update user status");
            }
        } catch (IOException e) {
            throw new AppRuntimeException("Failed to update user status");
        }
    }

    public CreateFormInitializationDataResponse getCreateFormInitializationData(Long id) {
        SessionObject session = SessionAttributes.current();
        User currentUser = session.getUser();
        CreateFormInitializationDataResponse response = new CreateFormInitializationDataResponse();
        if(id == null && session.getPermissions().contains(Actions.CREATE_OTHER_ACCOUNT_USER)) {
            response.clients = CrudDao.get(Client.class).query().eq("active", true).findAll();
        }
        if(!session.IS_GP) {
            Client client = currentUser.getClient();
            response.divisions = CrudDao.get(ClientDivision.class).query().eq("client", client).findAll();
        }
        response.user = id == null ? null : CrudDao.get(User.class).findOne(id);
        if(id == null || session.getPermissions().contains(Actions.CHANGE_ROLE)) {
            if(session.IS_GP && currentUser.getRole().getName().equals(SystemRoles.GP_ADMIN) || (currentUser.getRole().getInheritedFrom() != null && currentUser.getRole().getInheritedFrom().getName().equals(SystemRoles.GP_ADMIN))) { // GP Admin will be able to assign all roles only, otherwise other users only can assign inheritors
                    response.roles = CrudDao.get(Role.class).query()
                            .or()
                                .and()
                                    .nl("client")
                                    .eq("readonly", false)
                                .close()
                                .and()
                                    .eq("readonly", true)
                                    .eq("isForGp", true)
                                .close()
                            .close().findAll();
            } else if(!session.IS_GP && currentUser.getRole().getName().equals(SystemRoles.CLIENT_ADMIN) || (currentUser.getRole().getInheritedFrom() != null && currentUser.getRole().getInheritedFrom().getName().equals(SystemRoles.CLIENT_ADMIN))) {
            	Client client = currentUser.getClient();
                response.roles = CrudDao.get(Role.class).query()
                        .ne("name", SystemRoles.CLIENT_ADMIN)
                        .or()
                            .and()
                                .eq("client", client)
                                .eq("readonly", false)
                            .close()
                            .and()
                                .eq("readonly", true)
                                .eq("isForGp", false)
                            .close()
                        .close().findAll();
            } else {
                Role rootRole = currentUser.getRole().getInheritedFrom() == null ? currentUser.getRole() : currentUser.getRole().getInheritedFrom();
                response.roles = CrudDao.get(Role.class).query().or().eq("id", rootRole.getId()).eq("inheritedFrom", rootRole).close().findAll();
            }
        }
        return response;
    }

    @Transactional
    public void update(UserCreationRequest request) {
        Map<String, String> idpModifications = new HashMap<>();
        User operableUser = new User();
        SessionObject session = SessionAttributes.current();
        User sessionUser = session.getUser();
        if(request.id != null) {
            operableUser = CrudDao.get(User.class).findOne(request.id);
            if(operableUser == null) {
                throw new AppRuntimeException("Expected user not found", 404);
            }
        }

        long operableUserClientId = operableUser.getClient() == null ? 0 : operableUser.getClient().getId();
        long sessionUserClientId = sessionUser.getClient() == null ? 0 : sessionUser.getClient().getId();
        //for update only user from same account can be updated if not permitted. If permitted even then only client admin role users can be edited
        if(request.id != null) {
            if(operableUserClientId != sessionUserClientId) {
                if (!session.getPermissions().contains(Actions.EDIT_OTHER_ACCOUNT_USER)) {
                    throw new AppRuntimeException("You dont have permission to update user from other account", 403);
                }
                if(operableUser.getRole().getName().equals(SystemRoles.CLIENT_ADMIN)) {
                    throw new AppRuntimeException("You dont have permission to update non admin user from other account", 403);
                }
            }
        }
        if(request.accountId != null) {
            if(request.id != null) { //update
                if(request.accountId != operableUserClientId) {
                    throw new AppRuntimeException("Account for an existing user cannot be modified", 403);
                }
            } else { //create
                if((request.accountId != sessionUserClientId) && !session.getPermissions().contains(Actions.CREATE_OTHER_ACCOUNT_USER)) {
                    throw new AppRuntimeException("You dont have permission to create user for other account", 403);
                }
            }
            operableUser.setClient(CrudDao.get(Client.class).proxy(request.accountId));
            operableUserClientId = request.accountId;
        } else if(request.id == null && sessionUserClientId != 0) {
            operableUser.setClient(CrudDao.get(Client.class).proxy(sessionUserClientId));
            operableUserClientId = sessionUserClientId;
        } else {
            operableUserClientId = sessionUserClientId;
        }

        if(request.divisionIds != null) {
            if(operableUserClientId == 0 || operableUserClientId != sessionUserClientId) {
                throw new AppRuntimeException("Setting sub account is not allowed for this user", 403);
            }
            if(request.divisionIds.length > 0) {
                List<Long> operableUserClientDivisionIds = operableUser.getClient().getClientDivisions().stream().map(d -> d.getId()).collect(Collectors.toList());
                operableUserClientDivisionIds.retainAll(Arrays.asList(request.divisionIds));
                if(operableUserClientDivisionIds.size() != request.divisionIds.length) {
                    throw new AppRuntimeException("All the sub account should be from the same account that the user belongs to", 403);
                }
                Collection<ClientDivision> divisions = operableUser.getClientDivisions();
                CrudDao<ClientDivision> divisionDao = CrudDao.get(ClientDivision.class);
                Collection<ClientDivision> newDivisions = operableUserClientDivisionIds.stream().map(x -> divisionDao.proxy(x)).collect(Collectors.toList());
                divisions.retainAll(newDivisions);
                newDivisions.removeAll(divisions); 
                divisions.addAll(newDivisions);
                operableUser.setAllowAllDivision(false);
            } else {
                if(request.id != null) {
                    operableUser.getClientDivisions().retainAll(new ArrayList<>());
                }
                operableUser.setAllowAllDivision(true);
            }
        } else if(operableUserClientId != sessionUserClientId) {
            operableUser.setAllowAllDivision(true);
        }

        if(request.id == null) {
            if (StringUtil.isNullOrEmpty(request.name) || StringUtil.isNullOrEmpty(request.msisdn) || StringUtil.isNullOrEmpty(request.address)) {
                throw new AppRuntimeException("All the required parameters not given", 400);
            }
        }

        if(request.name != null) {
            if(request.id != null) {
                throw new AppRuntimeException("Name cannot be updated");
            }
            operableUser.setName(request.name);
            String[] names = request.name.split(" ");
            idpModifications.put("first_name", names[0].trim());
            if(names.length > 1) {
                idpModifications.put("last_name", request.name.substring(names[0].length()).trim());
            } else {
                idpModifications.put("last_name", names[0]);
            }
            idpModifications.put("display_name", request.name);
        }

        if(request.id != null && request.msisdn != null) {
            throw new AppRuntimeException("MSISDN cannot be updated");
        }
        if(request.id == null) {
            String msisdn = StringUtil.sanitizeMsisdn(request.msisdn);
            if (msisdn == null) {
                throw new AppRuntimeException("Invalid msisdn provided", 422);
            }
            int intMsisdn = Integer.parseInt(msisdn);
            if(isExist(null, "msisdn", intMsisdn)) {
                throw new AppRuntimeException("This msisdn already been used", 422);
            }
            idpModifications.put("mobile", "0" + msisdn);
            operableUser.setMsisdn(intMsisdn);
        }

        if (request.id == null) {
            if (session.IS_GP && operableUserClientId == 0) {
                if (request.email != null) {
                    throw new AppRuntimeException("Email is not applicable for GP User", 422);
                }
                request.email = request.adid + "@grameenphone.com";
            }
            if(isExist(null, "email", request.email)) {
                throw new AppRuntimeException("There is already a user with this email", 422);
            }
            operableUser.setEmail(request.email);
            operableUser.setLoginId(request.email);
            idpModifications.put("username", request.email);
            idpModifications.put("email", request.email);
        } else if (request.email != null && !request.email.equals(operableUser.getEmail())) {
            throw new AppRuntimeException("Email can not be updated", 422);
        }

        if(session.IS_GP) {
            if(request.id == null && operableUserClientId == 0 && StringUtil.isNullOrEmpty(request.adid)) {
                throw new AppRuntimeException("GP AD ID must be given for GP User", 422);
            }
            if(request.id != null && operableUserClientId == 0 && StringUtil.hasText(request.adid) && !operableUser.getAdid().equals(request.adid)) {
                throw new AppRuntimeException("GP AD ID cannot be updated", 422);
            }
            if(StringUtil.hasText(request.adid) && operableUserClientId != 0) {
                throw new AppRuntimeException("Non GP user cannot have GP Ad ID", 422);
            }
            if(request.id == null && operableUserClientId == 0) { //new user and not for other account
                if(isExist(null, "adid", request.adid)) {
                    throw new AppRuntimeException("This GP AD ID is already subscribed as a user", 422);
                }
                idpModifications.put("domain_id", request.adid);
                operableUser.setAdid(request.adid);
            }
        } else if(StringUtil.hasText(request.adid)) {
            throw new AppRuntimeException("Non GP User can not have GP Ad ID", 422);
        }

        if(StringUtil.hasText(request.address)) {
            operableUser.setAddress(request.address);
        }

        //roleChange detect
        boolean roleChanging = request.id != null && request.role != operableUser.getRole().getId();
        Role oldRole = operableUser.getRole();
        Role newRole = null;
        if(roleChanging && request.id == sessionUser.getId()) {
            throw new AppRuntimeException("One person cannot change his own role", 403);
        }
        if(roleChanging && !session.getPermissions().contains(Actions.CHANGE_ROLE)) {
            throw new AppRuntimeException("You dont have permission to change role for a user", 403);
        }
        CrudDao<Role> roleDao = CrudDao.get(Role.class);
        Role clientAdminRole = roleDao.query().eq("name", SystemRoles.CLIENT_ADMIN).findOne();
        if(request.id == null && request.role == null && operableUserClientId != sessionUserClientId) {
            operableUser.setRole(clientAdminRole);
            newRole = clientAdminRole;
        } else if(request.role != null) {
            if(request.id != null) {
                if(roleChanging && (operableUser.getRole().getName().equals(SystemRoles.CLIENT_ADMIN) || operableUser.getRole().getName().equals(SystemRoles.GP_ADMIN))) {
                    long adminUserCount = getActiveAdminCount(operableUser.getClient());
                    if(adminUserCount == 1) {
                        throw new AppRuntimeException("This user is the only admin, hence role cannot be changed", 422);
                    }
                }
            }
            if(!session.IS_GP && request.role == clientAdminRole.getId()) {
                throw new AppRuntimeException("Client Admin can not be set to any user", 422);
            }
            operableUser.setRole(roleDao.proxy(request.role));
            newRole = roleDao.query().eq("id", request.role).findOne();
        }
        if(operableUserClientId != sessionUserClientId && operableUser.getRole().getId() != clientAdminRole.getId()) {
            throw new AppRuntimeException("Only Client Admin role can be set for other account user", 422);
        }

        if (request.id == null) {
            HttpClient httpClient = new HttpClient();
            try {
                if (idpIsProxyRequired) {
                    httpClient.setDefaultProxy();
                }
                /*Response idpResponse = httpClient.invokeForEntity(HttpMethod.POST, idpUrlBase, HttpClient.serializeMap(idpModifications), new HashMap<>() {{
                    put("X-Api-Key", idpApiKey);
                    put("Content-Type", "application/x-www-form-urlencoded");
                }}, Response.class);
                if (!idpResponse.success) {
                    logger.error("User creation failed at idp");
                    throw new AppRuntimeException("Failed to create user at idp");
                }*/
                operableUser.setIdpUserId(10001);
            } catch (Exception e) {
                if(httpClient.getStatusCode() == 422) {
                    String response = httpClient.getTextResponse();
                    try {
                        Response idpResponse = Json.fromJson(response, Response.class);
                        List<String> errorFields = new ArrayList();
                        String combinedErrorFields = null;
                        if(idpResponse.errors != null) {
                            if(idpResponse.errors.get("email") != null && idpResponse.errors.get("email").contains("The email is already in-use.")) {
                                if(session.IS_GP) {
                                    errorFields.add("AD ID");
                                } else {
                                    errorFields.add("Email");
                                }
                            }
                            if(idpResponse.errors.get("mobile") != null && idpResponse.errors.get("mobile").contains("The mobile is already in-use.")) {
                                errorFields.add("MSISDN");
                            }
                            if(errorFields.size() == 2) {
                                combinedErrorFields = errorFields.get(0) + " and " + errorFields.get(1);
                            } else if(errorFields.size() == 1) {
                                combinedErrorFields = errorFields.get(0);
                            }
                        }
                        if(combinedErrorFields != null) {
                            if (session.IS_GP) {
                                throw new AppRuntimeException("User already exists in IDP/Single Sign-on System with this given " + combinedErrorFields + ". Please verify the details associated with user information in IDP");
                            } else {
                                throw new AppRuntimeException("A user ID already exists in GP Single Sign-on System with this given " + combinedErrorFields + ". Please contact with GP SPOC to verify or update user information.");
                            }
                        }
                        logger.error("User creation failed", e);
                    } catch (IOException ioException) {
                        logger.error("User creation failed", ioException);
                    }
                } else {
                    logger.error("User creation failed", e);
                }
                throw new AppRuntimeException("Failed to create user");
            }
        }

        userRepository.save(operableUser);

        if (request.id == null) {
            Event.fire(Events.USER_CREATE, operableUser);
        } else {
            Event.fire(Events.USER_UPDATE, operableUser);
        }

        if (roleChanging) {
            Event.fire(Events.USER_PERMISSIONS_CHANGE, operableUser, oldRole, newRole);
        }
    }

    public boolean isExist(Long id, String field, Object value) {
        return CrudDao.get(User.class).query().neif(() -> id != null, "id", id).eq(field, value).eq("deleted", false).count() > 0;
    }

    public void downloadAsXls(OutputStream outputStream) throws IOException {
        List<User> users = listableUsers(0, -1).records;
        ExcelWriter<UserExcelReport> excel = new ExcelWriter<>() {};
        excel.addHeader();
        excel.write(users.stream().map(b -> new UserExcelReport(b.getRole().getName(), b.getClient() == null ? "--" : b.getClient().getName(), b.getName(), b.getEmail(), b.getAddress(), b.isActive() ? "Active" : "Barred")).collect(Collectors.toList()));
        excel.flush(outputStream);
    }
}