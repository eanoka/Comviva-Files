package com.grameenphone.wipro.fmfs.mfs_communicator.model;

public class ServiceChargePaidAmount {
    public Double paidAmount;
    public Double serviceCharge;

    public ServiceChargePaidAmount(){

    }

    public ServiceChargePaidAmount(Double paidAmount, Double serviceCharge) {
        this.paidAmount = paidAmount;
        this.serviceCharge = serviceCharge;
    }
}
