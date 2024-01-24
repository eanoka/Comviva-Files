package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/**
 * @author wipro.tribhuwan
 */
@Entity
public class User {
    @Id
    @GeneratedValue(
            strategy = GenerationType.AUTO,
            generator = "native"
    )
    @GenericGenerator(
            name = "native",
            strategy = "native"
    )
    private long id;

    private String name;
    private String loginId;
    private String email;
    private String adid;
    private boolean active = true;
    private String address;
    private int msisdn;
    /**
     * If true then this user will have access for all the division the the client have
     */
    private boolean allowAllDivision;
    private int idpUserId;
    private Date lastLoginTime;
    /**
     * on delete loginid should be prefixed by timestamp to prevent further conflict with same loginid creation again
     */
    private boolean deleted;

    @ManyToOne()
    @Fetch(FetchMode.JOIN)
    private Role role;

    /**
     * It will be null for gp users
     */
    @ManyToOne()
    @Fetch(FetchMode.JOIN)
    private Client client; //Nullable

    @ManyToMany(fetch = FetchType.EAGER)
    @Fetch(FetchMode.JOIN)
    private Collection<ClientDivision> clientDivisions = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @JoinColumn(referencedColumnName = "id", name = "user_id")
    private Collection<UserAction> actions;

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

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Collection<ClientDivision> getClientDivisions() {
        return clientDivisions;
    }

    public void setClientDivisions(Collection<ClientDivision> clientDivisions) {
        this.clientDivisions = clientDivisions;
    }

    public Collection<UserAction> getActions() {
        return actions;
    }

    public void setActions(Collection<UserAction> actions) {
        this.actions = actions;
    }

    public boolean isAllowAllDivision() {
        return allowAllDivision;
    }

    public void setAllowAllDivision(boolean allowAllDivision) {
        this.allowAllDivision = allowAllDivision;
    }

    public int getIdpUserId() {
        return idpUserId;
    }

    public void setIdpUserId(int idpUserId) {
        this.idpUserId = idpUserId;
    }

    public Date getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Date lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public int getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(int mobile) {
        this.msisdn = mobile;
    }

    public String getAdid() {
        return adid;
    }

    public void setAdid(String adid) {
        this.adid = adid;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof User ? getId() != 0 ? getId() == ((User) obj).getId() : super.equals(obj) : super.equals(obj);
    }

    @Override
    public int hashCode() {
        return getId() != 0 ? (int)getId() : super.hashCode();
    }
}