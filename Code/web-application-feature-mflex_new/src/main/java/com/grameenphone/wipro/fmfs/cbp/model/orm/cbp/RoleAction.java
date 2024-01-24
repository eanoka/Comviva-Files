package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * @author wipro.tribhuwan
 *
 */
@Entity
public class RoleAction implements Serializable {

    private boolean allowed;
    private boolean denied;

    @Id
    @ManyToOne
    private Role role;

    @Id
    @ManyToOne
    private Action action;

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public boolean isDenied() {
        return denied;
    }

    public void setDenied(boolean denied) {
        this.denied = denied;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

}
