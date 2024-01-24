package com.grameenphone.wipro.fmfs.mfs_communicator.model.sms;

public class DPDPSMSSendRequest {
	
    private SmsAccesInfo accesInfo;
    private SmsInfo smsInfo;
    private Charge charge;
    private MessageInfo messageInfo;
    
    
    public Charge getCharge() {
		return charge;
	}

	public void setCharge(Charge charge) {
		this.charge = charge;
	}

	public MessageInfo getMessageInfo() {
		return messageInfo;
	}

	public void setMessageInfo(MessageInfo messageInfo) {
		this.messageInfo = messageInfo;
	}

	public SmsAccesInfo getAccesInfo() {
        return accesInfo;
    }

    public void setAccesInfo(SmsAccesInfo accesInfo) {
        this.accesInfo = accesInfo;
    }

    public SmsInfo getSmsInfo() {
        return smsInfo;
    }

    public void setSmsInfo(SmsInfo smsInfo) {
        this.smsInfo = smsInfo;
    }
}
