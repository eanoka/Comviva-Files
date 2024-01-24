package com.grameenphone.wipro.fmfs.cbp.model.orm.cbp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;

/**
 * @author wipro.tribhuwan
 */
@Entity
public class PaymentRequest {
    //region auto
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

    @Column(updatable = false, insertable = false)
    private Timestamp createTime;
    //endregion

    //region on creation
    @ManyToOne(fetch = FetchType.LAZY)
    private User addedBy;
    
	@ManyToOne(fetch = FetchType.LAZY)
	private Client client;
	 
	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH})
	private PaymentRequestHop lastHop;

    @OneToMany
    private Collection<ClientDivision> clientDivisions = new ArrayList<>();
    //endregion
        
    private String attachment;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public User getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(User addedBy) {
        this.addedBy = addedBy;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public PaymentRequestHop getLastHop() {
        return lastHop;
    }

    public void setLastHop(PaymentRequestHop lastHop) {
        this.lastHop = lastHop;
    }

    public Collection<ClientDivision> getClientDivisions() {
        return clientDivisions;
    }

    public void setClientDivisions(Collection<ClientDivision> clientDivisions) {
        this.clientDivisions = clientDivisions;
    }

	public String getAttachment() {
		return attachment;
	}

	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}
}