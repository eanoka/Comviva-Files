package javax.servlet.http;

import jakarta.servlet.http.HttpSession;

import java.util.Map;

public interface HttpServletRequest {
    Map getParameterMap();

    StringBuffer getRequestURL();

    String getQueryString();

    String getRequestURI();

    int getServerPort();

    String getScheme();

    String getServerName();

    boolean isSecure();

    String getParameter(String name);

    HttpSession getSession(boolean create);

    HttpSession getSession();
}