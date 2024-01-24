package com.grameenphone.wipro.fmfs.cbp.model.data.session;

import com.grameenphone.wipro.constants.EventScopes;
import com.grameenphone.wipro.constants.Events;
import com.grameenphone.wipro.fmfs.cbp.Application;
import com.grameenphone.wipro.fmfs.cbp.model.data.saml.Response;
import com.grameenphone.wipro.fmfs.cbp.model.data.websocket.SocketMessage;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import com.grameenphone.wipro.fmfs.cbp.service.NotificationService;
import com.grameenphone.wipro.utility.common.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

/**
 * This class is to hold all objects that is stored in session
 * {@link SessionAttributes#current()} can be called to get current stored instance in session
 */
public class SessionObject {
    private static NotificationService notificationService = Application.context.getBean(NotificationService.class);
    private final static Logger log = LoggerFactory.getLogger(SessionObject.class);

    public String ID;

    private User USER;
    private List<String> PERMISSIONS;
    public String FRONTEND_UNIQUE = UUID.randomUUID().toString().toUpperCase();
    public boolean IS_GP = false;

    private HttpSession session;

    public SessionObject() {}

    public SessionObject(HttpSession session) {
        this.session = session;
        ID = session.getId();
    }

    public void unsetUser() {
        if(session != null) {
            USER = null;
            SecurityContextHolder.getContext().setAuthentication(null);
            session.removeAttribute(SPRING_SECURITY_CONTEXT_KEY);
            Event.fire(Events.LOGOUT, EventScopes.SESSION, this);
        }
    }

    public void setUser(User user, List<String> permissions, boolean isGP, Response samlResponse) {
        if(session != null) {
            USER = user;
            PERMISSIONS = permissions;
            if (user == null) {
                unsetUser();
            } else {
                List<GrantedAuthority> authorities = permissions.stream().map(a -> new SimpleGrantedAuthority(a)).collect(Collectors.toList());
                SecurityContext context = SecurityContextHolder.getContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(user, samlResponse, authorities));
                session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);
                IS_GP = isGP;
                Event.one(Events.LOGOUT, ID, (o) -> {
                    try {
                        notificationService.notifyUser(((SessionObject)o).FRONTEND_UNIQUE, new SocketMessage().setTopic("session").setMessage("logout"));
                    } catch (Exception f) {
                        log.error("Could not send logout event");
                    }
                });
            }
        }
    }

    public User getUser() {
        return USER;
    }

    public List<String> getPermissions() {
        return PERMISSIONS;
    }

    public void setPermissions(List<String> permissions) {
        PERMISSIONS = permissions;
        SecurityContext context = (SecurityContext)session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken)context.getAuthentication();
        List<GrantedAuthority> authorities = permissions.stream().map(a -> new SimpleGrantedAuthority(a)).collect(Collectors.toList());
        context.setAuthentication(new UsernamePasswordAuthenticationToken(token.getPrincipal(), token.getCredentials(), authorities));
    }
}