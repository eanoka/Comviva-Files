package com.grameenphone.wipro.fmfs.cbp.model.data.session;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

public class SessionAttributes {
	private static final String SESSION_KEY = "__OBJECT_STORED_IN_SESSION__";

	public static SessionObject set(HttpSession session) {
		if(session == null) {
			return null;
		}
		try {
			RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
			SessionObject next = (SessionObject) attributes.getAttribute(SESSION_KEY, RequestAttributes.SCOPE_SESSION);
			if (next == null) {
				attributes.setAttribute(SESSION_KEY, next = new SessionObject(session), RequestAttributes.SCOPE_SESSION);
			}
			next.ID = attributes.getSessionId();
			return next;
		} catch (Throwable g) {
			return null;
		}
	}

	public static SessionObject ofSession(HttpSession session) {
		if(session == null) {
			return null;
		}
		return (SessionObject) session.getAttribute(SESSION_KEY);
	}

	/**
	 * Returns current session object.
	 * @return
	 */
	public static SessionObject current() {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if(attributes != null) {
			return (SessionObject) attributes.getAttribute(SESSION_KEY, RequestAttributes.SCOPE_SESSION);
		}
		return new SessionObject();
	}

	/**
	 * Returns current session object.
	 * @param shouldCreate if true and there is no session bound in current request then a session is created and objec is returned
	 * @return
	 */
	public static SessionObject current(boolean shouldCreate) {
		SessionObject object = current();
		if(object == null && shouldCreate) {
			ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			object = set(attr.getRequest().getSession(true));
		}
		return object;
	}
}