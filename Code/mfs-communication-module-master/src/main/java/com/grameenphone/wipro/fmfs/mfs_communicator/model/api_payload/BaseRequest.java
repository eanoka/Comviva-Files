package com.grameenphone.wipro.fmfs.mfs_communicator.model.api_payload;

import com.grameenphone.wipro.enums.Channel;
import com.grameenphone.wipro.enums.WalletType;

public abstract class BaseRequest {
    public String msisdn;
    public Channel channel;
    public WalletType wallet_type;
}