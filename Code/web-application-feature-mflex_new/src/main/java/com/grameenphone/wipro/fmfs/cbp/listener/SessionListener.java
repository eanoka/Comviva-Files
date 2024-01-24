package com.grameenphone.wipro.fmfs.cbp.listener;

import com.grameenphone.wipro.constants.EventScopes;
import com.grameenphone.wipro.fmfs.cbp.Application;
import com.grameenphone.wipro.constants.Events;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionObject;
import com.grameenphone.wipro.fmfs.cbp.model.data.websocket.SocketMessage;
import com.grameenphone.wipro.fmfs.cbp.service.NotificationService;
import com.grameenphone.wipro.utility.common.Event;
import com.grameenphone.wipro.utility.common.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@Component
public class SessionListener implements HttpSessionListener {
	private final static int TIMEOUT;
	private final static Logger log = LoggerFactory.getLogger(SessionListener.class);

	@Autowired
	NotificationService notificationService;

	static {
		TIMEOUT = StringUtil.hmTos(Application.environment.getProperty("session.timeout"));
		log.trace("Session Timeout is configured to be of " + TIMEOUT + " seconds");
	}

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		se.getSession().setMaxInactiveInterval(TIMEOUT);
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		SessionObject object = SessionAttributes.ofSession(se.getSession());
		Event.fire(Events.LOGOUT, se.getSession().getId(), object);
		Event.off("*", EventScopes.SESSION);
	}
}