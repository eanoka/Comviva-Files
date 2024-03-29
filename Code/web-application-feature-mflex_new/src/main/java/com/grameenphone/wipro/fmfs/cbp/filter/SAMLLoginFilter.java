package com.grameenphone.wipro.fmfs.cbp.filter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.server.PathContainer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

import com.grameenphone.wipro.extensions.spring.boot.beans.config.RequestMappingHandlerMapping;
import com.grameenphone.wipro.fmfs.cbp.Application;
import com.grameenphone.wipro.fmfs.cbp.config.SecurityConfig;
import com.grameenphone.wipro.fmfs.cbp.consts.RequestAttribute;
import com.grameenphone.wipro.fmfs.cbp.model.data.saml.Assertion;
import com.grameenphone.wipro.fmfs.cbp.model.data.saml.Attribute;
import com.grameenphone.wipro.fmfs.cbp.model.data.saml.Response;
import com.grameenphone.wipro.fmfs.cbp.model.data.session.SessionAttributes;
import com.grameenphone.wipro.fmfs.cbp.service.AuthService;
import com.grameenphone.wipro.utility.marshal.Json;
import com.grameenphone.wipro.utility.marshal.Xml;
import com.onelogin.saml2.Auth;
import com.onelogin.saml2.settings.Saml2Settings;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class SAMLLoginFilter extends GenericFilterBean {
	private final static Logger logger = LoggerFactory.getLogger(SAMLLoginFilter.class);

	String autoLoginId;
	AuthService authService;
	Saml2Settings saml2Settings;
	String samlCallbackUrl;

	static String basePathPrefix = Application.environment.getProperty("app.context");

	private RequestMatcher samlLoginUrlMatcher = new AntPathRequestMatcher(SecurityConfig.loginUrl);

	public SAMLLoginFilter(Saml2Settings saml2Settings, String samlCallbackUrl, AuthService authService, String autoLoginId) {
		this.authService = authService;
		this.autoLoginId = autoLoginId;
		this.saml2Settings = saml2Settings;
		this.samlCallbackUrl = samlCallbackUrl;
	}

	/**
	 * Auth generated by reflection - two classloader loading conflict arising
	 * @param request
	 * @param response
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	private Auth getJakartaCompatibleAuth(HttpServletRequest request, HttpServletResponse response) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		Class jakartaResponseInterface = Auth.class.getClassLoader().loadClass("javax.servlet.http.HttpServletResponse");
		Class jakartaResponseClass = Auth.class.getClassLoader().loadClass("javax.servlet.http.HttpServletResponseImpl");
		Object jakartaResponse = jakartaResponseClass.getConstructor(HttpServletResponse.class).newInstance(response);
		Class jakartaRequestInterface = Auth.class.getClassLoader().loadClass("javax.servlet.http.HttpServletRequest");
		Class jakartaRequestClass = Auth.class.getClassLoader().loadClass("javax.servlet.http.HttpServletRequestImpl");
		Object jakartaRequest = jakartaRequestClass.getConstructor(HttpServletRequest.class).newInstance(request);
		return Auth.class.getConstructor(Saml2Settings.class, jakartaRequestInterface, jakartaResponseInterface).newInstance(saml2Settings, jakartaRequest, jakartaResponse);
	}

	/**
	 * /login is excluded from this filter
	 * @param request
	 * @param response
	 * @param chain
	 * @throws IOException
	 * @throws ServletException
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpSession httpSession = ((HttpServletRequest)request).getSession(false);
		if(httpSession != null) {
			MDC.put("session", httpSession.getId());
		}
		SessionAttributes.set(httpSession);
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		try {
			if (!(authentication instanceof UsernamePasswordAuthenticationToken)) {
				String context = basePathPrefix == null ? ((HttpServletRequest) request).getContextPath() : basePathPrefix;
				String rootUrl = context + ("/".equals(context) ? "" : "/");
				String invalidLoginUrl = rootUrl + "static/page/invalidLogin.html";
				if(!RequestMappingHandlerMapping.ANONYMOUS_URLS.stream().anyMatch(u -> u.matches(PathContainer.parsePath(((HttpServletRequest) request).getServletPath())))) {
					if (autoLoginId == null || autoLoginId.equals("")) {
						if (((HttpServletRequest) request).getMethod().equals("GET")) {
							getJakartaCompatibleAuth((HttpServletRequest)request, (HttpServletResponse)response).login(samlCallbackUrl);
						} else if (samlLoginUrlMatcher.matches((HttpServletRequest) request) && request.getParameter("SAMLResponse") != null) {
							try {
								Auth auth = getJakartaCompatibleAuth((HttpServletRequest)request, (HttpServletResponse)response);
								auth.processResponse();
								if (auth.getErrors().size() == 0) {
									String responseXml = auth.getLastResponseXML();
									authService.authenticate(Xml.fromXml(responseXml, Response.class));
									((HttpServletResponse) response).sendRedirect(rootUrl);
									return;
								}
							} catch (Throwable ignored) {
							}
							((HttpServletResponse) response).sendRedirect(invalidLoginUrl);
							return;
						} else {
							int errorResponseStatus = 401;
							Map errorResponseObject = new HashMap() {{
								put("timestamp", System.currentTimeMillis());
								put("status", 401);
								put("message", "No Active Session. Please Login.");
							}};
							((HttpServletResponse) response).setStatus(errorResponseStatus);
							response.setContentType("application/json");
							try {
								response.getOutputStream().write(Json.toJson(errorResponseObject).getBytes());
							} catch (IOException e) {
							}
						}
						return;
					} else {
						try {
							//authenticate session with configured auto login id
							authService.authenticate(new Response() {{
								Assertion = new Assertion() {{
									AttributeStatement = new ArrayList<>() {{
										add(new Attribute() {{
											Name = "username";
											AttributeValue = autoLoginId;
										}});
									}};
								}};
							}});
						} catch (Exception p) {
							if(p instanceof RuntimeException) {
								logger.debug("Failed To Authenticate: ", p);
							}
							((HttpServletResponse) response).sendRedirect(invalidLoginUrl);
							return;
						}
					}
				}
			}
		} catch (Throwable j) {
			request.setAttribute(RequestAttribute.FILTER_EXCEPTION, j);
		}
		chain.doFilter(request, response);
	}
}