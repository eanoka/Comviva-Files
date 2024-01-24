package com.grameenphone.wipro.fmfs.mfs_communicator.filter;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.grameenphone.wipro.enums.RequestAttribute;
import com.grameenphone.wipro.exception.ServiceProcessingError;
import com.grameenphone.wipro.exception.HttpErrorResponseException;
import com.grameenphone.wipro.exception.TaggedCheckedException;
import com.grameenphone.wipro.utility.common.HttpClient;
import com.grameenphone.wipro.utility.marshal.Json;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.NestedServletException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(2)
public class ExceptionFilter extends GenericFilterBean {
	protected final static Logger log = LoggerFactory.getLogger(HttpClient.class);

	/**
	 * @param request
	 * @param response
	 * @param chain
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
		Map errorResponseObject;
		int errorResponseStatus;

		try {
			Throwable exceptionFromEarlierFilter = (Throwable) request.getAttribute(RequestAttribute.FILTER_EXCEPTION);
			if(exceptionFromEarlierFilter != null) {
				throw exceptionFromEarlierFilter;
			}
			chain.doFilter(request, response);
			return;
		} catch (Throwable t) {
			if(t instanceof NestedServletException) {
				t = t.getCause();
			}
			if(t instanceof TaggedCheckedException) {
				t = t.getCause();
			}
			if(t instanceof HttpMessageNotReadableException) {
				errorResponseStatus = 405;
				String message;
				if(t.getCause() instanceof InvalidFormatException) {
					InvalidFormatException _t = (InvalidFormatException) t.getCause();
					message = _t.getValue() + " is not a supported value for " + _t.getPath().get(0).getFieldName();
				} else {
					message = "Unable to parse request body";
				}
				errorResponseObject = new HashMap() {{
					put("timestamp", System.currentTimeMillis());
					put("status", 405);
					put("message", message);
				}};
			} else if(t instanceof HttpErrorResponseException) {
				errorResponseStatus = ((HttpErrorResponseException) t).getStatus();
				HttpErrorResponseException _t = (HttpErrorResponseException) t;
				errorResponseObject = new HashMap() {{
					put("timestamp", System.currentTimeMillis());
					put("status", errorResponseStatus);
					put("message", _t.getMessage());
				}};
			} else if(t instanceof ServiceProcessingError) {
				ServiceProcessingError _t = (ServiceProcessingError) t;
				errorResponseStatus = _t.status;
				errorResponseObject = new HashMap() {{
					put("timestamp", System.currentTimeMillis());
					put("status", _t.status);
					put("message", _t.getMessage());
				}};
			} else {
				String reference = RandomStringUtils.randomAlphanumeric(20);
				log.error("Error Occurred At Path " + "(" + reference + ") " + ((HttpServletRequest) request).getRequestURI(), t);
				errorResponseStatus = 500;
				errorResponseObject = new HashMap() {{
					put("timestamp", System.currentTimeMillis());
					put("status", 500);
					put("message", "Server Error");
					put("reference", reference);
				}};
			}
		}
		((HttpServletResponse)response).setStatus(errorResponseStatus);
		response.setContentType("application/json");
		try {
			response.getOutputStream().write(Json.toJson(errorResponseObject).getBytes());
		} catch (IOException e) {
		}
	}
}
