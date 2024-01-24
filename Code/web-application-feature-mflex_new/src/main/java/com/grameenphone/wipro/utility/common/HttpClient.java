package com.grameenphone.wipro.utility.common;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.fmfs.cbp.Application;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.marshal.Xml;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author wipro.zobair
 * @updated 11-11-21
 */
public class HttpClient {
    protected final static Logger log = LoggerFactory.getLogger(HttpClient.class);
    public static boolean isMockEnabled;
    private String proxyHost;
    private int proxyPort;
    private Integer timeout = 10;
    private Boolean hardTimeout = true;
    private HttpResponse response;
    private String textResponse;
    private int statusCode;
    private String sslContext;
    private boolean noExceptionForError = false;
    private boolean maskHeaders = true;
    private HttpClientBuilder clientBuilder;
    private CookieStore cookieStore = new BasicCookieStore();
    private X509Certificate clientCertificate;
    private PrivateKey clientKey;
    private PayloadLoggerInterceptor payloadLoggerInterceptor;

	private final static String defaultProxyHost;
	private final static int defaultProxyPort;

	static {
		defaultProxyHost = Application.environment.getProperty("http_proxy");
		defaultProxyPort = Integer.parseInt(Application.environment.getProperty("http_port", "0"));
		isMockEnabled = "test".equals(System.getProperty("execution.profile"));
    }

    public static class HttpRequestSnapshot {
        public String body;

        public HttpRequestSnapshot(String body) {
            this.body = body;
        }
    }

    public static class HttpResponseSnapshot {
        public int status;
        public String reason;
        public String body;

        public HttpResponseSnapshot(int status, String reason, String body) {
            this.status = status;
            this.reason = reason;
            this.body = body;
        }
    }

    public interface PayloadLoggerInterceptor {
        default PayloadLoggerInterceptor getChainInterceptor() {return null;}
        default String intercept(String url) {
			PayloadLoggerInterceptor chainInterceptor = getChainInterceptor();
			if(chainInterceptor != null) {
				return chainInterceptor.intercept(url);
			}
			return url;
		}
        default void intercept(Map<String, String> headers) {
			PayloadLoggerInterceptor chainInterceptor = getChainInterceptor();
			if(chainInterceptor != null) {
				chainInterceptor.intercept(headers);
			}
		}
        default String intercept(HttpRequestSnapshot payload) {
			PayloadLoggerInterceptor chainInterceptor = getChainInterceptor();
			if(chainInterceptor != null) {
				return chainInterceptor.intercept(payload);
			}
			return payload.body;
        }
        default String intercept(HttpResponseSnapshot payload) {
			PayloadLoggerInterceptor chainInterceptor = getChainInterceptor();
			if(chainInterceptor != null) {
				return chainInterceptor.intercept(payload);
			}
			return payload.body;
        }
    }

    public interface HttpMethod {
        String DELETE = "delete";
        String GET = "get";
        String HEAD = "head";
        String OPTIONS = "options";
        String PATCH = "patch";
        String POST = "post";
        String PUT = "put";
        String TRACE = "trace";
    }

    public HttpClient() {}

    public HttpClient(Integer timeout) {
        this.timeout = timeout;
    }

    public HttpClient(String proxy, int proxyPort) {
        this.proxyHost = proxy;
        this.proxyPort = proxyPort;
    }

    public HttpClient(String proxy, int proxyPort, Integer timeout) {
        this.proxyHost = proxy;
        this.proxyPort = proxyPort;
        this.timeout = timeout;
    }

    public void setNoExceptionForError(boolean yesno) {
        noExceptionForError = yesno;
    }

	public interface Function_<A, B> extends Function<A, B> {}
	public interface Function__<A, B> extends Function<A, B> {}

	public <T> void setPayloadLoggerInterceptor(Function<String, String> interceptor) {
		PayloadLoggerInterceptor currentInterceptor = payloadLoggerInterceptor;
		payloadLoggerInterceptor = new PayloadLoggerInterceptor() {
			@Override
			public PayloadLoggerInterceptor getChainInterceptor() {
				return currentInterceptor;
			}

			@Override
			public String intercept(String url) {
				return PayloadLoggerInterceptor.super.intercept(interceptor.apply(url));
			}
		};
	}

	public void setPayloadLoggerInterceptor(Function_<HttpRequestSnapshot, String> interceptor) {
		PayloadLoggerInterceptor currentInterceptor = payloadLoggerInterceptor;
		payloadLoggerInterceptor = new PayloadLoggerInterceptor() {
			@Override
			public PayloadLoggerInterceptor getChainInterceptor() {
				return currentInterceptor;
			}

			@Override
			public String intercept(HttpRequestSnapshot payload) {
				String returnable = interceptor.apply(payload);
				payload.body = returnable;
				return PayloadLoggerInterceptor.super.intercept(payload);
			}
		};
	}

	public void setPayloadLoggerInterceptor(Function__<HttpResponseSnapshot, String> interceptor) {
		PayloadLoggerInterceptor currentInterceptor = payloadLoggerInterceptor;
		payloadLoggerInterceptor = new PayloadLoggerInterceptor() {
			@Override
			public PayloadLoggerInterceptor getChainInterceptor() {
				return currentInterceptor;
			}

			@Override
			public String intercept(HttpResponseSnapshot payload) {
				String returnable = interceptor.apply(payload);
				payload.body = returnable;
				return PayloadLoggerInterceptor.super.intercept(payload);
			}
		};
	}

	public void setPayloadLoggerInterceptor(Consumer<Map<String, String>> interceptor) {
		PayloadLoggerInterceptor currentInterceptor = payloadLoggerInterceptor;
		payloadLoggerInterceptor = new PayloadLoggerInterceptor() {
			@Override
			public PayloadLoggerInterceptor getChainInterceptor() {
				return currentInterceptor;
			}

			@Override
			public void intercept(Map<String, String> headers) {
				interceptor.accept(headers);
				PayloadLoggerInterceptor.super.intercept(headers);
			}
		};
    }

    public void setClientAuth(X509Certificate clientCertificate, PrivateKey clientKey) {
        this.clientCertificate = clientCertificate;
        this.clientKey = clientKey;
    }

    private KeyManager[] getKeyManagers() {
        return clientCertificate == null ? null : new KeyManager[]{new X509KeyManager() {
            @Override
            public String[] getClientAliases(String s, Principal[] principals) {
                return new String[0];
            }

            @Override
            public String chooseClientAlias(String[] strings, Principal[] principals, Socket socket) {
                return "client";
            }

            @Override
            public String[] getServerAliases(String s, Principal[] principals) {
                return new String[0];
            }

            @Override
            public String chooseServerAlias(String s, Principal[] principals, Socket socket) {
                return null;
            }

            @Override
            public X509Certificate[] getCertificateChain(String s) {
                return new X509Certificate[]{clientCertificate};
            }

            @Override
            public PrivateKey getPrivateKey(String s) {
                return clientKey;
            }
        }};
    }

    private org.apache.http.client.HttpClient getHttpClient(URI url, boolean doLog) throws NoSuchAlgorithmException {
        clientBuilder = clientBuilder == null ? HttpClientBuilder.create() : clientBuilder;
        if(timeout != null) {
            RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout * 1000).setConnectionRequestTimeout(timeout * 1000).setSocketTimeout(timeout * 1000).build();
            clientBuilder.setDefaultRequestConfig(config);
        }
        if(proxyHost != null) {
            if(doLog) {
                log.trace("Using Proxy: " + proxyHost + ":" + proxyPort);
            }
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            clientBuilder.setRoutePlanner(routePlanner);
        }

        if(url.getScheme().toLowerCase().equals("https")) {
            SSLContext context = null;
            if(sslContext != null) {
                context = SSLContext.getInstance(sslContext);
            }
            try {
                if(context == null) {
                    context = SSLContexts.custom().build();
                }
                context.init(getKeyManagers(), new X509TrustManager[]{new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, new SecureRandom());
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(context, NoopHostnameVerifier.INSTANCE);
                Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", new PlainConnectionSocketFactory())
                        .register("https", sslsf)
                        .build();
                PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
                cm.setMaxTotal(100);
                clientBuilder.setSSLSocketFactory(sslsf).setConnectionManager(cm);
            } catch (Exception j) {
                log.error("Error building http client ", j);
            }
        }
        clientBuilder.setDefaultCookieStore(cookieStore);
        org.apache.http.client.HttpClient client = clientBuilder.build();
        if(isMockEnabled) {
            return MockHttpClient.mockIt(client);
        }
        return client;
    }

    private org.apache.http.client.HttpClient getHttpClientWithHeader(HttpRequestBase http, Map<String, String> headers, boolean doLog) throws IOException {
        org.apache.http.client.HttpClient client;
        try {
            client = getHttpClient(http.getURI(), doLog);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Could not connect to url");
        }
        if(headers != null) {
            for (String key : headers.keySet()) {
                http.addHeader(key, headers.get(key));
            }
        }
        return client;
    }

    public void setSslContext(String context) {
        sslContext = context;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public void setTimeout(Integer timeout, boolean isHard) {
        this.timeout = timeout;
        hardTimeout = isHard;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public String getTextResponse() {
        return textResponse;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setDefaultProxy() {
		this.proxyHost = defaultProxyHost;
		this.proxyPort = defaultProxyPort;
    }

    private HttpRequestBase getRequestBase(String method, String url) {
        switch(method) {
            case HttpMethod.DELETE:
                return new HttpDelete(url);
            case HttpMethod.GET:
                return new HttpGet(url);
            case HttpMethod.HEAD:
                return new HttpHead(url);
            case HttpMethod.OPTIONS:
                return new HttpOptions(url);
            case HttpMethod.PATCH:
                return new HttpPatch(url);
            case HttpMethod.POST:
                return new HttpPost(url);
            case HttpMethod.PUT:
                return new HttpPut(url);
            case HttpMethod.TRACE:
                return new HttpTrace(url);
        }
        return null;
    }

    private void logResponse() {
        HttpResponseSnapshot responsePayload = new HttpResponseSnapshot(statusCode, response.getStatusLine().getReasonPhrase(), textResponse);
        String loggableResponse = textResponse;
        if(payloadLoggerInterceptor != null) {
            payloadLoggerInterceptor.intercept(responsePayload);
        }
        log.trace("STATUS: " + responsePayload.status);
        if (statusCode != 200) {
            log.trace("REASON: " + responsePayload.reason);
        }
        log.trace("RESPONSE: " + loggableResponse);
    }

    private void logRequest(String body) {
        if(payloadLoggerInterceptor != null) {
            body = payloadLoggerInterceptor.intercept(new HttpRequestSnapshot(body));
        }
        log.trace("REQUEST: " + body);
    }

    private void logUrl(String url) {
        if(payloadLoggerInterceptor != null) {
            url = payloadLoggerInterceptor.intercept(url);
        }
        log.trace("URL: " + url);
    }

    private void logHeaders(Map<String, String> headers) {
        if(headers != null || headers.size() > 0) {
            if(payloadLoggerInterceptor != null) {
                headers = new HashMap<>(headers);
                payloadLoggerInterceptor.intercept(headers);
            } else {
                if(maskHeaders) {
                    Map<String, String> _headers = new HashMap<>(headers);
                    headers.forEach((k, v) -> {_headers.put(k, k.toLowerCase().matches(".*(key|secret|auth|pass|id|code).*") ? "*****" : v);});
                    headers = _headers;
                }
            }
            log.trace("HEADERS: " + headers);
        }
    }

    private void executeRequest(HttpRequestBase get, org.apache.http.client.HttpClient client) throws IOException {
        boolean[] httpComplete = new boolean[] {false};
        Timer timeoutTimer = null;
        if(hardTimeout && timeout != null) {
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if(!httpComplete[0]) {
                        get.abort();
                    }
                }
            };
            timeoutTimer = new Timer(true);
            timeoutTimer.schedule(task, (timeout + 2) * 1000);
        }
        response = client.execute(get);
        if(hardTimeout && timeout != null) {
            timeoutTimer.cancel();
        }
        httpComplete[0] = true;
    }

    private String getTextFromResponse(HttpResponse response) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.getEntity().writeTo(outputStream);
        statusCode = response.getStatusLine().getStatusCode();
        textResponse = new String(outputStream.toByteArray());

        if(!noExceptionForError) {
            if (statusCode / 100 != 2) {
                throw new HttpErrorResponseException(statusCode, "Request returned error: " + response.getStatusLine().getReasonPhrase(), textResponse);
            }
        }
        return textResponse;
    }

	private void clearInvocationDetail() {
		response = null;
		statusCode = -1;
		textResponse = null;
	}

    public String get(String url) throws IOException {
        return get(url, new HashMap());
    }

    public String get(String url, Map headers) throws IOException {
        return get(url, headers, true);
    }

    public String get(String url, Map<String, String> headers, boolean doLog) throws IOException {
        return invoke(HttpMethod.GET, url, null, headers, doLog);
    }

    public <T> T getForEntity(String url, Class<T> clazz) throws IOException {
        return getForEntity(url, new HashMap<>(), clazz);
    }

    public <T> T getForEntity(String url, Map<String, String> headers, Class<T> clazz) throws IOException {
        return getForEntity(url, headers, clazz, true);
    }

    public <T> T getForEntity(String url, Map<String, String> headers, Class<T> clazz, boolean log) throws IOException {
        return invokeForEntity(HttpMethod.GET, url, null, headers, clazz, log);
    }

    public String post(String url, String body) throws IOException {
        return post(url, body, new HashMap());
    }

    public String post(String url, String body, Map<String, String> headers) throws IOException {
        return post(url, body, headers, true);
    }

    public String post(String url, String body, Map<String, String> headers, boolean doLog) throws IOException {
        return invoke(HttpMethod.POST, url, body, headers, doLog);
    }

	public String post(String url, Object body) throws IOException {
		return post(url, body, new HashMap());
	}
	public String post(String url, Object body, Map<String, String> headers) throws IOException {
		return post(url, body, headers, true);
	}
	public String post(String url, Object body, Map<String, String> headers, boolean doLog) throws IOException {
		return invoke(HttpMethod.POST, url, convertObjectBodyEntityToString(body, headers), headers, doLog);
	}
    public <T> T postForEntity(String url, String body, Class<T> clazz) throws IOException {
        return postForEntity(url, body, new HashMap<String, String>(), clazz);
    }

    public <T> T postForEntity(String url, String body, Map<String, String> headers, Class<T> clazz) throws IOException {
        return postForEntity(url, body, headers, clazz, true);
    }

    public <T> T postForEntity(String url, String body, Map<String, String> headers, Class<T> clazz, boolean log) throws IOException {
        return invokeForEntity(HttpMethod.POST, url, (Object) body, headers, clazz, log);
    }

    public <T> T postForEntity(String url, Object body, Class<T> clazz) throws IOException {
        return postForEntity(url, body, new HashMap<>(), clazz);
    }

    public <T> T postForEntity(String url, Object body, Map<String, String> headers, Class<T> clazz) throws IOException {
        return postForEntity(url, body, headers, clazz, true);
    }

    public <T> T postForEntity(String url, Object body, Map<String, String> headers, Class<T> clazz, boolean log) throws IOException {
        return invokeForEntity(HttpMethod.POST, url, body, headers, clazz, log);
    }

    public String invoke(String method, String url, String body) throws IOException {
        return invoke(method, url, body, new HashMap());
    }

    public String invoke(String method, String url, String body, Map<String, String> headers) throws IOException {
        return invoke(method, url, body, headers, true);
    }

    public String invoke(String method, String url, String body, Map<String, String> headers, boolean doLog) throws IOException {
		clearInvocationDetail(); //To clear earlier invocation data if any
        if(doLog) {
            logUrl(url);
            logHeaders(headers);
        }
        final HttpRequestBase post = getRequestBase(method, url);
        org.apache.http.client.HttpClient client = getHttpClientWithHeader(post, headers, doLog);
        if(post instanceof HttpEntityEnclosingRequestBase) {
            ((HttpEntityEnclosingRequestBase) post).setEntity(new StringEntity(body));
            if(doLog) {
                logRequest(body);
            }
        }
		try {
        executeRequest(post, client);
            return getTextFromResponse(response);
        } finally {
			if(response != null) {
                if(doLog) {
                    logResponse();
                }
			} else {
				log.debug("ABORT: No Response Fetched");
			}
        }
    }

    public <T> T invokeForEntity(String method, String url, String body, Class<T> clazz) throws IOException {
        return invokeForEntity(method, url, body, new HashMap<>(), clazz);
    }

    public <T> T invokeForEntity(String method, String url, String body, Map<String, String> headers, Class<T> clazz) throws IOException {
        return invokeForEntity(method, url, body, headers, clazz, true);
    }

    public <T> T invokeForEntity(String method, String url, String body, Map<String, String> headers, Class<T> clazz, boolean log) throws IOException {
        return invokeForEntity(method, url, (Object)body, headers, clazz, log);
    }

    public <T> T invokeForEntity(String method, String url, Object body, Class<T> clazz) throws IOException {
        return invokeForEntity(method, url, body, new HashMap<>(), clazz);
    }

    public <T> T invokeForEntity(String method, String url, Object body, Map<String, String> headers, Class<T> clazz) throws IOException {
        return invokeForEntity(method, url, body, headers, clazz, true);
    }

    public <T> T invokeForEntity(String method, String url, Object body, Map<String, String> headers, Class<T> clazz, boolean log) throws IOException {
		String response = invoke(method, url, convertObjectBodyEntityToString(body, headers), headers, log);
		if(response.replaceFirst("\\s+", "").startsWith("<")) {
			return Xml.fromXml(response, clazz);
		}
		return Json.fromJson(response, clazz);
	}

    private String convertObjectBodyEntityToString(Object body, Map<String, String> headers) throws IOException {
        String stringBody;
        if (body != null) {
            if (body instanceof String) {
                stringBody = (String) body;
            } else {
                String typeHeader = headers.get("Content-Type");
                if (typeHeader == null) {
                    if (body.getClass().isAnnotationPresent(JacksonXmlRootElement.class)) {
                        stringBody = Xml.toXml(body);
                        headers.put("Content-Type", MediaType.TEXT_XML_VALUE);
                    } else {
                        stringBody = Json.toJson(body);
                        headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
                    }
                } else if (MediaType.TEXT_XML_VALUE.equals(typeHeader)) {
                    stringBody = Xml.toXml(body);
                } else if (MediaType.APPLICATION_JSON_VALUE.equals(typeHeader)) {
                    stringBody = Json.toJson(body);
                } else if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(typeHeader)) {
                    if (body instanceof Map) {
                        stringBody = serializeMap((Map) body);
                    } else {
                        stringBody = serializeMap(Json.fromJson(Json.toJson(body), Map.class));
                    }
                } else {
                    stringBody = body.toString();
                }
            }
        } else {
            stringBody = "";
        }
        return stringBody;
    }

    public static String serializeMap(Map<String, ? extends Object> map) {
        return serializeMap(map, true);
    }

    public static String serializeMap(Map<String, ? extends Object> map, boolean encode) {
        if (map.size() == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        TriConsumer<String, String, Integer> appender = encode ? (k, s, i) -> {
            if (i > 1) {
                builder.append("&" + k + "=");
            }
            if (!s.trim().equals("")) {
                try {
                    builder.append(URLEncoder.encode(s, "UTF-8").replace("+", "%20"));
                } catch (UnsupportedEncodingException e) {
                }
            }
        } : (k, s, i) -> {
            if (i > 1) {
                builder.append("&" + k + "=");
            }

            if (!s.trim().equals("")) {
                builder.append(s);
            }
        };
        for (String key : map.keySet()) {
            builder.append("&" + key + "=");
            Object value = map.get(key);
            if (value != null) {
                if (value instanceof String) {
                    appender.accept(key, (String) value, 1);
                } else if (value instanceof Collection) {
                    int i = 0;
                    for (Object _value : (Collection) value) {
                        if (_value != null) {
                            appender.accept(key, _value.toString(), ++i);
                        }
                    }
                } else if (value.getClass().isArray()) {
                    int i = 0;
                    for (; i < Array.getLength(value); i++) {
                        Object _value = Array.get(value, i);
                        if (_value != null) {
                            appender.accept(key, _value.toString(), i + 1);
                        }
                    }
                }
            }
        }
        return builder.substring(1);
    }
}