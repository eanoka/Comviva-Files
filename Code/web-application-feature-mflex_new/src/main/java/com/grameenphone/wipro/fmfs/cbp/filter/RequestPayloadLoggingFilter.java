package com.grameenphone.wipro.fmfs.cbp.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.grameenphone.wipro.annot.PaylodLoggerInterceptor;
import com.grameenphone.wipro.annot.PaylodLoggerInterceptors;
import com.grameenphone.wipro.utility.common.HttpClient;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author wipro.zobair
 * @updated 11-11-21
 */
public class RequestPayloadLoggingFilter implements Filter {
    private final static Logger logger = LoggerFactory.getLogger(RequestPayloadLoggingFilter.class);
    private List<AntPathRequestMatcher> doNotLogUrls = List.of(new AntPathRequestMatcher("/api/saml/**"), new AntPathRequestMatcher("/api/cbp-notification-socket/**"));
    private List<AntPathRequestMatcher>  loggableUrls = List.of(new AntPathRequestMatcher("/api/**"));
    private String headerKey = "X-Forwarded-For";
    private List<Interceptor> interceptors = new ArrayList<>();

    private Integer payloadLogLimit;

    private static class Interceptor {
        public RequestMatcher urlMatcher;
        public BiFunction<String, Boolean, String> interceptor;
    }

    public RequestPayloadLoggingFilter(Integer payloadLogLimit) {
        this.payloadLogLimit = payloadLogLimit;
        try {
            Reflections reflections = new Reflections("com.grameenphone.wipro.fmfs.cbp");
            List<PaylodLoggerInterceptor> paylodLoggerInterceptors = new ArrayList<>();
            for(Class annotatedClasses : reflections.getTypesAnnotatedWith(PaylodLoggerInterceptors.class)) {
                PaylodLoggerInterceptors paylodLoggerInterceptors_ = ((PaylodLoggerInterceptors)annotatedClasses.getDeclaredAnnotation(PaylodLoggerInterceptors.class));
                for(PaylodLoggerInterceptor paylodLoggerInterceptor : paylodLoggerInterceptors_.value()) {
                    paylodLoggerInterceptors.add(paylodLoggerInterceptor);
                }
            }
            for(Class annotatedClasses : reflections.getTypesAnnotatedWith(PaylodLoggerInterceptor.class)) {
                paylodLoggerInterceptors.add((PaylodLoggerInterceptor) annotatedClasses.getDeclaredAnnotation(PaylodLoggerInterceptor.class));
            }
            for(PaylodLoggerInterceptor paylodLoggerInterceptor : paylodLoggerInterceptors) {
                Interceptor interceptor = new Interceptor();
                interceptor.urlMatcher = new AntPathRequestMatcher(paylodLoggerInterceptor.pattern());
                interceptor.interceptor = paylodLoggerInterceptor.interceptor().getDeclaredConstructor().newInstance();
                interceptors.add(interceptor);
            }
        } catch(Throwable t) {}
    }

    protected String getMessagePayload(ContentCachingResponseWrapper wrapper) {
        return getMessagePayload(wrapper.getContentAsByteArray(), wrapper.getCharacterEncoding());
    }

    private String getMessagePayload(byte[] contentAsByteArray, String characterEncoding) {
        if (contentAsByteArray.length > 0) {
            int length = Math.min(contentAsByteArray.length, payloadLogLimit);
            try {
                return new String(contentAsByteArray, 0, length, characterEncoding);
            } catch (UnsupportedEncodingException ex) {
                return "[unknown]";
            }
        }
        return null;
    }

    protected void logRequestMessage(HttpServletRequest request) {
        StringBuilder msg = new StringBuilder();
        msg.append("REQUEST: uri=").append(request.getRequestURI());
        msg.append("; method=").append(request.getMethod());

        List<BiFunction<String, Boolean, String>> matchedInterceptor = new ArrayList<>();
        for (Interceptor i : interceptors) {
            if (i.urlMatcher.matches(request)) {
                matchedInterceptor.add(i.interceptor);
            }
        }

        String queryString = request.getQueryString();
        if (queryString != null) {
            for(BiFunction<String, Boolean, String> interceptor : matchedInterceptor) {
                queryString = interceptor.apply(queryString, true);
            }
            msg.append('?').append(queryString);
        }

        String client = request.getRemoteAddr();
        String headerValue = request.getHeader(headerKey);
        if (StringUtils.hasText(headerValue)) {
            msg.append("; client=").append(headerValue);
        } else if (StringUtils.hasText(client)) {
            msg.append("; client=").append(client);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            msg.append("; session=").append(session.getId());
        }
        String user = request.getRemoteUser();
        if (user != null) {
            msg.append("; user=").append(user);
        }
        String payload = "GET".equals(request.getMethod()) ? null : request instanceof LoggableRequestWrapper ? ((LoggableRequestWrapper)request).getCachedContent() : null;
        if (payload != null) {
            for(BiFunction<String, Boolean, String> interceptor : matchedInterceptor) {
                payload = interceptor.apply(payload, true);
            }
            msg.append("; payload=").append(payload);
        }
        String requestBody = msg.toString();
        logger.debug(requestBody);
    }

    protected void logResponseMessage(HttpServletRequest request, ContentCachingResponseWrapper response, long takenTime) {
        StringBuilder msg = new StringBuilder();
        msg.append("RESPONSE: time=").append(takenTime).append("ms");
        msg.append("; status=").append(response.getStatus());
        List<BiFunction<String, Boolean, String>> matchedInterceptor = new ArrayList<>();
        for (Interceptor i : interceptors) {
            if (i.urlMatcher.matches(request)) {
                matchedInterceptor.add(i.interceptor);
            }
        }
        String payload = getMessagePayload(response);
        if (payload != null) {
            for(BiFunction<String, Boolean, String> interceptor : matchedInterceptor) {
                payload = interceptor.apply(payload, false);
            }
            msg.append("; payload=").append(payload);
        }
        String responseBody = msg.toString();
        logger.debug(responseBody);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletRequest _request = request;
        if(doNotLogUrls.stream().anyMatch(p -> p.matches((HttpServletRequest) _request)) || loggableUrls.stream().noneMatch(p -> p.matches((HttpServletRequest) _request))) {
            chain.doFilter(request, response);
            return;
        }
        response = new ContentCachingResponseWrapper((HttpServletResponse) response);
        if ("GET".equals(((HttpServletRequest) request).getMethod()) || request.getContentLength() == 0) {
            logRequestMessage((HttpServletRequest) request);
        } else {
            request = new LoggableRequestWrapper((HttpServletRequest) request);
        }
        long startTime = new Date().getTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long takenTime = new Date().getTime() - startTime;
            logResponseMessage((HttpServletRequest) request, (ContentCachingResponseWrapper)response, takenTime);
            ((ContentCachingResponseWrapper)response).copyBodyToResponse();
        }
    }

    private class LoggableInputStream extends ServletInputStream {
        private final ServletInputStream is;
        private LoggableRequestWrapper baseRequest;

        private boolean doneTriggered = false;
        private ByteArrayBuilder cachedContent = new ByteArrayBuilder();

        public LoggableInputStream(LoggableRequestWrapper request, ServletInputStream is) {
            this.is = is;
            baseRequest = request;
        }

        @Override
        public int read() throws IOException {
            int ch = is.read();
            writeToCache(new byte[] {(byte) ch}, 0, ch == -1 ? -1 : 1);
            return ch;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int count = is.read(b);
            writeToCache(b, 0, count);
            return count;
        }

        private void writeToCache(final byte[] b, final int off, int count) {
            if (doneTriggered) {
                return;
            }
            if(count == -1) {
                    logRequestMessage(baseRequest);
                    doneTriggered = true;
            } else {
                int countToWrite = payloadLogLimit - cachedContent.size();
                    cachedContent.write(b, off, Math.min(countToWrite, count));
                    countToWrite -= count;
                if (countToWrite <= 0) {
                    logRequestMessage(baseRequest);
                    doneTriggered = true;
                }
            }
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            int count = this.is.read(b, off, len);
            writeToCache(b, off, count);
            return count;
        }

        @Override
        public int readLine(final byte[] b, final int off, final int len) throws IOException {
            int count = this.is.readLine(b, off, len);
            writeToCache(b, off, count);
            return count;
        }

        @Override
        public boolean isFinished() {
            return this.is.isFinished();
        }

        @Override
        public boolean isReady() {
            return this.is.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            this.is.setReadListener(readListener);
        }
        @Override
        public void close() throws IOException {
            if(!doneTriggered) {
                logRequestMessage(baseRequest);
                doneTriggered = true;
            }
            super.close();
        }
    }

    private class LoggableRequestWrapper extends HttpServletRequestWrapper {
        private LoggableInputStream is;
        private String contentType;
        private boolean isParamLogged;

        public LoggableRequestWrapper(HttpServletRequest request) {
            super(request);
            contentType = getContentType();
            if (contentType == null) {
                contentType = "";
            }
            int semicolon = contentType.indexOf(';');
            if (semicolon >= 0) {
                contentType = contentType.substring(0, semicolon).trim();
            } else {
                contentType = contentType.trim();
            }
        }

        private boolean isBodyLoggable() {
            return !"multipart/form-data".equals(contentType) && "POST".equals(getMethod()) && !"application/x-www-form-urlencoded".equals(contentType);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (!isBodyLoggable()) {
                return super.getInputStream();
            }
            if(is == null) {
                is = new LoggableInputStream(this, super.getInputStream());
            }
            return is;
        }

        private String getSerializedFormParameters() {
            if (!"application/x-www-form-urlencoded".equals(contentType) || getContentLength() < 1) {
                return "";
            }
            StringBuffer buffer = new StringBuffer();
            buffer.append("[Serialized Body]");
            Map<String, String[]> form = super.getParameterMap();
            buffer.append(HttpClient.serializeMap(form));
            return buffer.toString();
        }

        @Override
        public String getParameter(String name) {
            if (!isParamLogged) {
                isParamLogged = true;
                logRequestMessage(this);
            }
            return super.getParameter(name);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            if (!isParamLogged) {
                isParamLogged = true;
                logRequestMessage(this);
            }
            return super.getParameterMap();
        }

        @Override
        public Enumeration<String> getParameterNames() {
            if (!isParamLogged) {
                isParamLogged = true;
                logRequestMessage(this);
            }
            return super.getParameterNames();
        }

        @Override
        public String[] getParameterValues(String name) {
            if (!isParamLogged) {
                isParamLogged = true;
                logRequestMessage(this);
            }
            return super.getParameterValues(name);
        }
        public String getCachedContent() {
            return isParamLogged ? getSerializedFormParameters() : is == null ? "" : new String(is.cachedContent.toByteArray());
        }
    }
}