package com.grameenphone.wipro.enums;

public enum DpdcResponseFlag {
	
	SUCCESSFUL(1),INVALID_AMOUNT(2),EMPTY(3);
	
	private int value;
	
	DpdcResponseFlag(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}
	
}
