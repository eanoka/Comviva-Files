package com.grameenphone.wipro.fmfs.cbp.service;

import com.grameenphone.wipro.constants.Events;
import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.fmfs.cbp.consts.Actions;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Action;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Client;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.RoleAction;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.model.view.role.AllowDeny;
import com.grameenphone.wipro.fmfs.cbp.model.view.role.PaginatedRole;
import com.grameenphone.wipro.fmfs.cbp.model.view.role.UpdatePermissionRequest;
import com.grameenphone.wipro.fmfs.cbp.repository.CrudDao;
import com.grameenphone.wipro.fmfs.cbp.repository.cbp.RoleRepository;
import com.grameenphone.wipro.utility.common.Event;
import com.grameenphone.wipro.utility.orm.WhereBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleService {
    @Autowired
    private RoleRepository roleRepository;

    public void createRole(Role role) {
        if(isNameExist(role.getName(), null)) {
            throw new AppRuntimeException("The name you provided already exist");
        }
        if(!inheritableRoles().contains(role.getInheritedFrom())) {
            throw new AppRuntimeException("Custom role must inherit some pre defined roles");
        }
        User user = SessionAttributes.current().getUser();
        role.setClient(user.getClient());
        roleRepository.save(role);
    }

    public void updateRole(Long id, Long inheritedFrom, String name) {
        if(isNameExist(name, id)) {
            throw new AppRuntimeException("The name you provided already exist");
        }
        Role role = CrudDao.get(Role.class).findOne(id);
        if(role == null) {
            throw new AppRuntimeException("Expected role could not be found", 404);
        }
        Role inheritedRole = CrudDao.get(Role.class).proxy(inheritedFrom);
        if(!inheritableRoles().contains(inheritedRole)) {
            throw new AppRuntimeException("Custom role must inherit some pre defined roles");
        }
        SessionObject session = SessionAttributes.current();
        Client client = session.getUser().getClient();
        if(client == null) {
            if(role.getClient() != null) {
                throw new AppRuntimeException("Unauthorized role", 403);
            }
        } else if(!client.equals((role.getClient()))) {
            throw new AppRuntimeException("Unauthorized role", 403);
        }
        if(role.getInheritedFrom().getId() != inheritedFrom && !session.getPermissions().contains(Actions.CHANGE_ROLE)) {
            throw new AppRuntimeException("You don't have permission to change inheritance");
        }
        role.setInheritedFrom(inheritedRole);
        role.setName(name);
        roleRepository.save(role);
    }

    public void deleteRole(long id) {
        if(!deletableRoles().contains(new Object() {
            @Override
            public boolean equals(Object obj) {
                return id == ((Role)obj).getId();
            }
        })) {
            throw new AppRuntimeException("You are not authorized to delete this role", 403);
        }
        roleRepository.deleteById(id);
    }

    public boolean isNameExist(String name, Long id) {
        Client client = SessionAttributes.current().getUser().getClient();
        return CrudDao.get(Role.class).query()
                .neif(() -> id != null, "id", id)
                .eq("name", name)
                .orif(() -> client != null) // Non GP roles
                    .and()
                        .nl("client")
                        .eq("isForGp", false)
                        .close()
                    .eq("client", client)
                    .close()
                .orif(() -> client == null) //GP Roles
                    .eq("isForGp", true)
                    .and()
                        .nl("isForGp")
                        .nl("client")
                        .close()
                    .close().count() > 0;
    }

    /**
     * Non system roles those are from same client
     * @return
     */
    public List<Role> editableRoles() {
        SessionObject session = SessionAttributes.current();
        Client client = session.getUser().getClient();
        return CrudDao.get(Role.class).query().eq("readonly", false).eq("client", client).ne("id", session.getUser().getRole().getId()).findAll();
    }

    /**
     * Those accessible non system roles that does not been assigned to any user
     * @return
     */
    public List<Role> deletableRoles() {
        SessionObject session = SessionAttributes.current();
        Client client = session.getUser().getClient();
        return CrudDao.get(Role.class).query().eq("readonly", false).eq("client", client).sub(User.class).eqf("role.id", ":root.id").ne().findAll();
    }

    /**
     * All the non system roles of same account and system roles that are for GP for GP roles and for NOn GP Non gp roles
     * @param offset
     * @param perPage
     * @return
     */
    public PaginatedRole listableRoles(long offset, int perPage) {
        Client client = SessionAttributes.current().getUser().getClient();
        WhereBuilder<Role, ?> roleQuery = CrudDao.get(Role.class).query()
                .orif(() -> client != null) // Non GP roles
                    .and()
                        .nl("client")
                        .eq("isForGp", false)
                        .close()
                    .eq("client", client)
                    .close()
                .orif(() -> client == null) //GP Roles
                    .eq("isForGp", true)
                    .and()
                        .nl("isForGp")
                        .nl("client")
                        .close()
                    .close();
        long count = roleQuery.count();
        if(count <= offset) {
            offset = (long)(Math.ceil(count / (double)perPage) - 1) * perPage;
        }
        List<Role> records;
        if(count == 0) {
            offset = 0;
            records = new ArrayList<>();
        } else {
            records = roleQuery.findAll(offset, perPage);
        }
        PaginatedRole paginatedRoleData = new PaginatedRole();
        paginatedRoleData.count = count;
        paginatedRoleData.offset = offset;
        paginatedRoleData.perPage = perPage;
        paginatedRoleData.records = records;
        return paginatedRoleData;
    }

    /**
     * @return system roles that are for GP for GP roles and for NOn GP Non gp roles
     */
    public List<Role> inheritableRoles() {
        SessionObject session = SessionAttributes.current();
        return CrudDao.get(Role.class).query().eq("readonly", true).eq("isForGp", session.IS_GP).findAll();
    }

    public Role getRole(long id) {
        return CrudDao.get(Role.class).findOne(id);
    }

    public List<AllowDeny> getOwnPermissionsOnly(Role role) {
        return role.getActions().stream().map(s -> {
            AllowDeny allowDeny = new AllowDeny();
            allowDeny.name = s.getAction().getName();
            allowDeny.allow = s.isAllowed();
            allowDeny.deny = s.isDenied();
            return allowDeny;
        }).collect(Collectors.toList());
    }

    public List<AllowDeny> getCumulativePermissions(Role role) {
        List<AllowDeny> allowDenies = getOwnPermissionsOnly(role);
        if(!role.isReadonly()) {
            allowDenies.addAll(role.getInheritedFrom().getActions().stream().map(s -> {
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
            }).collect(Collectors.toList()));
        }
        return allowDenies;
    }

    @Transactional
    public void updatePermissions(UpdatePermissionRequest request) {
        Role role = CrudDao.get(Role.class).findOne(request.id);
        if(!editableRoles().contains(role)) {
            throw new AppRuntimeException("You are not permitted to manage this role");
        }
        List<Action> allActions = CrudDao.get(Action.class).findAll();
        Collection<RoleAction> roleActions = role.getActions();
        List<String> applicablePermissions = SessionAttributes.current().getPermissions();
        CrudDao<RoleAction> roleActionCrudDao = CrudDao.get(RoleAction.class);
        request.changes.forEach((x, y) -> {
            if(!applicablePermissions.contains(x)) {
                throw new AppRuntimeException("You are not permitted for one of these permissions to change");
            }
            Optional<RoleAction> optionalRoleAction = roleActions.stream().filter(r -> r.getAction().getName().equals(x)).findFirst();
            RoleAction roleAction;
            if(optionalRoleAction.isEmpty()) {
                roleAction = new RoleAction();
                int index = allActions.indexOf(new Object() {
                    @Override
                    public boolean equals(Object obj) {
                        return x.equals(((Action)obj).getName());
                    }
                });
                if(index < 0) {
                    throw new AppRuntimeException("Invalid Permission");
                }
                roleAction.setRole(role);
                roleAction.setAction(allActions.get(index));
                if(y.equals("I")) {
                    return;
                }
                roleActionCrudDao.save(roleAction);
            } else {
                roleAction = optionalRoleAction.get();
                if(y.equals("I")) {
                    roleActionCrudDao.delete(roleAction);
                    return;
                }
                roleAction.setAllowed(false);
                roleAction.setDenied(false);
            }
            if(y.equals("A")) {
                roleAction.setAllowed(true);
            } else if(y.equals("D")) {
                roleAction.setDenied(true);
            }
            roleActionCrudDao.save(roleAction);
        });
        Event.fire(Events.ROLE_PERMISSIONS_CHANGE, role);
    }
}