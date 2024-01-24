package com.grameenphone.wipro.spring.boot.beans.config;

import com.grameenphone.wipro.annot.PinContainingRequest;
import com.grameenphone.wipro.fmfs.mfs_communicator.filter.PayloadLoggerInterceptorRegister;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

public class RequestMappingHandlerMapping extends org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping {
	Field patternsField;

	{
		try {
			patternsField = PatternsRequestCondition.class.getDeclaredField("patterns");
			patternsField.setAccessible(true);
		} catch (NoSuchFieldException e) {
		}
	}

	@Override
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		RequestMappingInfo baseInfo = super.getMappingForMethod(method, handlerType);
		if (!handlerType.getPackageName().startsWith("com.grameenphone")) {
			return baseInfo;
		}
		baseInfo = getUpdatedMappingInfo(baseInfo, method, handlerType);
		if(method.isAnnotationPresent(PinContainingRequest.class)) {
			PayloadLoggerInterceptorRegister.registerInterceptorFor(baseInfo.getPatternsCondition().getPatterns());
		}
		return baseInfo;
	}

	private RequestMappingInfo getUpdatedMappingInfo(RequestMappingInfo baseInfo, Method method, Class<?> handlerType) {
		if (baseInfo == null) {
			baseInfo = createRequestMappingInfo(handlerType);
			if (baseInfo == null) {
				String className = handlerType.getSimpleName();
				String firstPart = className.substring(0, 1).toLowerCase() + className.substring(1, className.length() - 10);
				String secondPart = method.getName();
				RequestMapping mapping = new PathOnlyRequestMapping("/" + firstPart + "/" + secondPart);
				return createRequestMappingInfo(mapping, null);
			} else {
				addPattern(baseInfo, method, handlerType);
				return baseInfo;
			}
		} else {
			Set<String> basePatterns = baseInfo.getPatternsCondition().getPatterns();
			if (basePatterns.isEmpty()) {
				addPattern(baseInfo, method, handlerType);
			} else if (basePatterns.size() == 1 && basePatterns.iterator().next().equals("")) {
				try {
					patternsField.set(baseInfo.getPatternsCondition(), new LinkedHashSet<>());
				} catch (IllegalAccessException e) {
				}
				addPattern(baseInfo, method, handlerType);
			} else {
				RequestMappingInfo info = createRequestMappingInfo(method);
				if (info.getPatternsCondition().getPatterns().isEmpty()) {
					addPattern(baseInfo, method, handlerType);
				}
			}
			return baseInfo;
		}
	}

	private void addPattern(RequestMappingInfo baseInfo, Method method, Class<?> handlerType) {
		String secondPart = method.getName();
		PatternsRequestCondition patternsRequestCondition = baseInfo.getPatternsCondition();
		Set<String> basePatterns = patternsRequestCondition.getPatterns();
		Set<String> updatedPatterns = new LinkedHashSet<>();
		try {
			patternsField.set(patternsRequestCondition, updatedPatterns);
		} catch (IllegalAccessException e) {
		}
		if (basePatterns.isEmpty()) {
			String className = handlerType.getSimpleName();
			//lower the first character and remove Controller
			String firstPart = className.substring(0, 1).toLowerCase() + className.substring(1, className.length() - 10);
			updatedPatterns.add("/" + firstPart + "/" + secondPart);
		} else {
			for (String pattern : basePatterns) {
				updatedPatterns.add(pattern + (pattern.endsWith("/") ? "" : "/") + secondPart);
			}
		}
	}

	private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
		return (requestMapping != null ? createRequestMappingInfo(requestMapping, null) : null);
	}
}