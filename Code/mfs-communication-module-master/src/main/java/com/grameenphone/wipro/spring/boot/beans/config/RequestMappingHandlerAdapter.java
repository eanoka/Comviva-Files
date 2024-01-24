package com.grameenphone.wipro.spring.boot.beans.config;

import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import com.grameenphone.wipro.enums.RequestAttribute;
import org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

public class RequestMappingHandlerAdapter extends org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter {
	/**
	 * adding model and method in request attribute
	 * @param handlerMethod
	 * @return
	 */
	@Override
	protected ServletInvocableHandlerMethod createInvocableHandlerMethod(HandlerMethod handlerMethod) {
		return new ServletInvocableHandlerMethod(handlerMethod) {
			@Override
			public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {
				webRequest.setAttribute(RequestAttribute.REQUEST_CURRENT_MODEL, mavContainer.getModel(), SCOPE_REQUEST);
				webRequest.setAttribute(RequestAttribute.HANDLER_METHOD, handlerMethod, SCOPE_REQUEST);
				super.invokeAndHandle(webRequest, mavContainer, providedArgs);
			}
		};
	}
}