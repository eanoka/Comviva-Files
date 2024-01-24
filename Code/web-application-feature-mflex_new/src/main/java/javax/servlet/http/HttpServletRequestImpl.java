package javax.servlet.http;

import jakarta.servlet.http.HttpSession;

import java.util.Map;

public class HttpServletRequestImpl implements HttpServletRequest {
    final private jakarta.servlet.http.HttpServletRequest request;

    public HttpServletRequestImpl(jakarta.servlet.http.HttpServletRequest orgRequest) {
        request = orgRequest;
    }

    public Map getParameterMap() {
        return request.getParameterMap();
    }

    public StringBuffer getRequestURL() {
        return request.getRequestURL();
    }

    public String getQueryString() {
        return request.getQueryString();
    }

    public String getRequestURI() {
        return request.getRequestURI();
    }

    public int getServerPort() {
        return request.getServerPort();
    }

    public String getScheme() {
        return request.getScheme();
    }

    public String getServerName() {
        return request.getServerName();
    }

    public boolean isSecure() {
        return request.isSecure();
    }

    public String getParameter(String name) {
        return request.getParameter(name);
    }

    public HttpSession getSession(boolean create) {
        return request.getSession(create);
    }

    public HttpSession getSession() {
        return request.getSession();
    }
}