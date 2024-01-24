package com.grameenphone.wipro.fmfs.mfs_communicator.filter;

import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.grameenphone.wipro.fmfs.mfs_communicator.component.PayloadPinRemover;
import com.grameenphone.wipro.utility.common.HttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

@Component
@Order(1)
public class RequestResponseLoggingFilter extends GenericFilterBean {
    private final static Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    @Autowired
    PayloadPinRemover pinRemover;
    private String headerKey = "X-Forwarded-For";
    @Value("${request.log.payload.max.size}")
    private Integer payloadLogLimit;

    protected String getMessagePayload(LoggableRequestWrapper request) {
        String payload = request.getCachedContent();
        if (PayloadLoggerInterceptorRegister.isMatch(request.getRequestURI().substring(request.getContextPath().length()))) {
            payload = pinRemover.sanitizedPayload(payload);
        }
        return payload;
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

        String queryString = request.getQueryString();
        if (queryString != null) {
            msg.append('?').append(queryString);
        }

        String client = request.getRemoteAddr();
        String headerValue = request.getHeader(headerKey);
        if (StringUtils.isNotEmpty(headerValue)) {
            msg.append("; client=").append(headerValue);
        } else if (StringUtils.isNotEmpty(client)) {
            msg.append("; client=").append(client);
        }
        String user = request.getRemoteUser();
        if (user != null) {
            msg.append("; user=").append(user);
        }
        String payload = "GET".equals(request.getMethod()) ? null : request instanceof LoggableRequestWrapper ? getMessagePayload((LoggableRequestWrapper) request) : null;
        if (payload != null) {
            msg.append("; payload=").append(payload);
        }
        String requestBody = msg.toString();
        logger.debug(requestBody);
    }

    protected void logResponseMessage(ContentCachingResponseWrapper response, long takenTime) {
        StringBuilder msg = new StringBuilder();
        msg.append("RESPONSE: time=").append(takenTime).append("ms");
        msg.append("; status=").append(response.getStatus());
        String payload = getMessagePayload(response);
        if (payload != null) {
            msg.append("; payload=").append(payload);
        }
        String responseBody = msg.toString();
        logger.debug(responseBody);
    }

    private boolean isResponseLoggable(HttpServletRequest request) {
        return true;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        long startTime = new Date().getTime();
        response = new ContentCachingResponseWrapper((HttpServletResponse) response);
        if ("GET".equals(((HttpServletRequest) request).getMethod()) || request.getContentLength() == 0) {
            logRequestMessage((HttpServletRequest) request);
        } else {
            request = new LoggableRequestWrapper((HttpServletRequest) request);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            if (isResponseLoggable((HttpServletRequest) request)) {
                long takenTime = new Date().getTime() - startTime;
                logResponseMessage((ContentCachingResponseWrapper) response, takenTime);
                ((ContentCachingResponseWrapper) response).copyBodyToResponse();
            }
        }
    }

    private class LoggableInputStream extends ServletInputStream {
        private final ServletInputStream is;
        private LoggableRequestWrapper baseRequest;

        private boolean doneTriggered = false;
        private ByteArrayBuilder cachedContent = new ByteArrayBuilder();
        private int totalCountToWrite;
        private byte[] singleByteWrapper = new byte[]{0};

        public LoggableInputStream(LoggableRequestWrapper request, ServletInputStream is, int contentLength) {
            this.is = is;
            baseRequest = request;
            totalCountToWrite = contentLength == -1 ? payloadLogLimit : Math.min(contentLength, payloadLogLimit);
        }

        @Override
        public int read() throws IOException {
            int ch = is.read();
            singleByteWrapper[0] = (byte) ch;
            writeToCache(singleByteWrapper, 0, ch == -1 ? -1 : 1);
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
            if (count == -1) {
                logRequestMessage(baseRequest);
                doneTriggered = true;
            } else {
                int countToWrite = totalCountToWrite - cachedContent.size();
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
        private int contentLength;
        private String contentType;
        private boolean isParamLogged;

        public LoggableRequestWrapper(HttpServletRequest request) {
            super(request);
            contentLength = request.getContentLength();
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
            if (is == null) {
                is = new LoggableInputStream(this, super.getInputStream(), contentLength);
            }
            return is;
        }

        private String getSerializedFormParameters() {
            if (!"application/x-www-form-urlencoded".equals(contentType) || getContentLength() < 1) {
                return "";
            }
            StringBuffer buffer = new StringBuffer();
            buffer.append("[Deserialized Body]");
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