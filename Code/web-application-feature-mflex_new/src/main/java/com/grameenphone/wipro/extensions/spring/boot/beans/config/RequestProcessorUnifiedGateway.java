package com.grameenphone.wipro.extensions.spring.boot.beans.config;

import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.fmfs.cbp.resolver.SessionObjectArgumentResolver;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.marshal.Json;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;
import org.springframework.web.util.NestedServletException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.grameenphone.wipro.fmfs.cbp.consts.RequestAttribute.FILTER_EXCEPTION;
import static com.grameenphone.wipro.fmfs.cbp.consts.RequestAttribute.HANDLER_METHOD;
import static com.grameenphone.wipro.fmfs.cbp.consts.RequestAttribute.REQUEST_CURRENT_MODEL;
import static com.grameenphone.wipro.fmfs.cbp.consts.RequestAttribute.REQUEST_MODEL_VIEW_CONTAINER;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

/**
 * To set request body model in request attribute and generically process all exception. Didn't use controller advice and exception filter to get exclusive control on exception handling
 */
public class RequestProcessorUnifiedGateway extends org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter {
	protected final static Logger log = LoggerFactory.getLogger(HttpClient.class);

	/**
	 * adding model and method in request attribute
	 * @param handlerMethod
	 * @return
	 */
	@Override
	protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
		return new ServletInvocableHandlerMethod(handlerMethod) {
			@Override
			public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer, Object... providedArgs) {
				webRequest.setAttribute(REQUEST_MODEL_VIEW_CONTAINER, mavContainer, SCOPE_REQUEST);
				webRequest.setAttribute(REQUEST_CURRENT_MODEL, mavContainer.getModel(), SCOPE_REQUEST);
				webRequest.setAttribute(HANDLER_METHOD, handlerMethod, SCOPE_REQUEST);

				Map errorResponseObject;
				int errorResponseStatus;

				try {
					Throwable exceptionFromEarlierFilter = (Throwable) webRequest.getAttribute(FILTER_EXCEPTION, SCOPE_REQUEST);
					if(exceptionFromEarlierFilter != null) {
						throw exceptionFromEarlierFilter;
					}
					super.invokeAndHandle(webRequest, mavContainer, providedArgs);
					return;
				} catch (Throwable t) {
					//TODO: have to show html page for get request
					if(t instanceof NestedServletException) {
						t = t.getCause();
					}
					if(t instanceof AccessDeniedException) {
						errorResponseStatus = 403;
						errorResponseObject = new HashMap() {{
							put("timestamp", System.currentTimeMillis());
							put("status", 403);
							put("message", "Access Denied");
						}};
					} else if(t instanceof AuthenticationException) {
						errorResponseStatus = 401;
						errorResponseObject = new HashMap() {{
							put("timestamp", System.currentTimeMillis());
							put("status", 401);
							put("message", "No Active Session. Please Login.");
						}};
					} else if(t instanceof HttpErrorResponseException) {
						errorResponseStatus = ((HttpErrorResponseException) t).getStatus();
						HttpErrorResponseException _t = (HttpErrorResponseException) t;
						errorResponseObject = new HashMap() {{
							put("timestamp", System.currentTimeMillis());
							put("status", _t.getStatus());
							put("message", _t.getReason());
						}};
					} else if(t instanceof AppRuntimeException) {
						errorResponseStatus = ((AppRuntimeException) t).status;
						AppRuntimeException _t = (AppRuntimeException) t;
						errorResponseObject = new HashMap() {{
							put("timestamp", System.currentTimeMillis());
							put("status", _t.status);
							put("message", _t.getMessage());
						}};
					} else if(t instanceof HttpMessageNotReadableException) {
						errorResponseStatus = 400;
						errorResponseObject = new HashMap() {{
							put("timestamp", System.currentTimeMillis());
							put("status", 400);
							put("message", "Invalid Data Submitted");
						}};
					} else {
						String reference = RandomStringUtils.randomAlphanumeric(20);
						String errorPath = webRequest.getRequest().getRequestURI();
						String context = webRequest.getRequest().getContextPath();
						if((("/".equals(context) ? "" : context) + "/error").equals(errorPath)) {
							log.error("Error Occurred In Earlier Request");
							errorResponseStatus = 404;
							errorResponseObject = new HashMap() {{
								put("timestamp", System.currentTimeMillis());
								put("status", 404);
								put("message", "Error Occurred At Server");
							}};
						} else {
							log.error("Error Occurred At Path (" + reference + ") " + errorPath, t);
							errorResponseStatus = 500;
							errorResponseObject = new HashMap() {{
								put("timestamp", System.currentTimeMillis());
								put("status", 500);
								put("message", "Server Error. Reference: " + reference);
							}};
						}
					}
				}
				webRequest.getResponse().setStatus(errorResponseStatus);
				webRequest.getResponse().setContentType("application/json");
				try {
					webRequest.getResponse().getOutputStream().write(Json.toJson(errorResponseObject).getBytes());
				} catch (IOException e) {
				}
				mavContainer.setRequestHandled(true);
			}
		};
	}

	@Override
	/**
	 * Session Object resolver placed here to place this resolver on first position otherwise possibility is there that this will be handled by other resolver
	 */
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		List<HandlerMethodArgumentResolver> resolvers = getArgumentResolvers().stream().collect(Collectors.toList());
		resolvers.add(0, new SessionObjectArgumentResolver());
		setArgumentResolvers(resolvers);
	}
}