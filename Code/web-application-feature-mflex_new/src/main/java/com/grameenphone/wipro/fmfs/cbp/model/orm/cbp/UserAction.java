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
public class UserAction implements Serializable {
    private boolean allowed;
    private boolean denied;

    @Id
    @ManyToOne
    private User user;

    @Id
    @ManyToOne
    private Action action;

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allow) {
        this.allowed = allow;
    }

    public boolean isDenied() {
        return denied;
    }

    public void setDenied(boolean deny) {
        this.denied = deny;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }
}