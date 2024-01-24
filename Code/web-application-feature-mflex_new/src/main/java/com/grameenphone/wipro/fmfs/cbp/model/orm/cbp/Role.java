package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.io.Serializable;
import java.util.Collection;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * @author wipro.tribhuwan
 */
@Entity
public class Role implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private long id;

    private String name;

    /**
     * These are system provided roles
     */
    private boolean readonly = false;

    /**
     * System provided values will be classified by this. If its true then these system roles are for GP otherwise for some clients mentioned in client field
     */
    private Boolean isForGp;

    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;

    @OneToMany
    @JoinColumn(referencedColumnName = "id", name = "role_id")
    private Collection<RoleAction> actions;

    /**
     * Every customize role must inherit from any system role. Whether any custom role is for gp or not will be decided by isForGp field of inheritedFrom
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private Role inheritedFrom;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Collection<RoleAction> getActions() {
        return actions;
    }

    public void setActions(Collection<RoleAction> actions) {
        this.actions = actions;
    }

    public Role getInheritedFrom() {
        return inheritedFrom;
    }

    public void setInheritedFrom(Role inheritedFrom) {
        this.inheritedFrom = inheritedFrom;
    }

    public Boolean isForGp() {
        return isForGp;
    }

    public void setForGp(Boolean forGp) {
        isForGp = forGp;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Role ? getId() != 0 ? getId() == ((Role) obj).getId() : super.equals(obj) : super.equals(obj);
    }

    @Override
    public int hashCode() {
        return getId() != 0 ? (int)getId() : super.hashCode();
    }
}