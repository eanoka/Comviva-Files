package com.grameenphone.wipro.fmfs.cbp.model.data.websocket;

import com.grameenphone.wipro.fmfs.cbp.enums.SocketMessageType;

public class SocketMessage {
	public String topic;
	public Object data;
	public SocketMessageType type = SocketMessageType.success;
	public String message;

	public SocketMessage setTopic(String topic) {
		this.topic = topic;
		return this;
	}

	public SocketMessage setData(Object data) {
		this.data = data;
		return this;
	}

	public SocketMessage setMessage(String message) {
		this.message = message;
		return this;
	}

	public SocketMessage setMessageType(SocketMessageType type) {
		this.type = type;
		return this;
	}
}