package com.grameenphone.wipro.fmfs.cbp.model.orm.report;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * @author wipro.tribhuwan
 */
@Entity
public class Transaction {
	@Id
	@GeneratedValue(
			strategy = GenerationType.AUTO,
			generator = "native"
	)
	@GenericGenerator(
			name = "native",
			strategy = "native"
	)
	public Long id;
	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@Fetch(FetchMode.JOIN)
	public Utility company;
	public String accountNo;
	public String billNo;
	public String amount;
	public Double serviceCharge;
	public Double paidAmount;
	@Column(nullable = false)
	public Date paymentDate;
	public String thirdPartyTxnid;
	public String mfsTxnid;
	public String reversalTxnid;
	@Column(nullable = false)
	public String status;
	public String initiator;
	@Column(nullable = false)
	public String transactionType;
	@Column(nullable = false)
	public String channel;
	@Column(nullable = false)
	public String customerMsisdn;
	@Column(nullable = false)
	public String payerMsisdn;
	public String responseCode;
	@Column(nullable = false)
	public long mfsBillPayTableId;
	@Column(nullable = false)
	public Date updateDate;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "transaction_id", referencedColumnName = "id", updatable = false, insertable = false)
	public Collection<TransactionExtraData> extraData = new ArrayList<>();
}