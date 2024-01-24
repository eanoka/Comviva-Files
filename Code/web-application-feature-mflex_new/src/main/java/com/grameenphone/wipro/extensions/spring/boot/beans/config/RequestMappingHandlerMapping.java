package com.grameenphone.wipro.extensions.spring.boot.beans.config;

import com.grameenphone.wipro.annot.AllowAnonymous;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.util.pattern.PathPattern;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Customize spring default handler mapping to create mapping automatically with controller name and action name.
 * If a controller name is AbcController, pqr is the action name and no mapping is defined in annotation for this class then /api/abc/pqr will be the auto generated mapping for that action
 */
public class RequestMappingHandlerMapping extends org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping {
	Field patternsField;

	public final static List<PathPattern> ANONYMOUS_URLS = new ArrayList<>();

	{
		try {
			patternsField = PathPatternsRequestCondition.class.getDeclaredField("patterns");
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
		RequestMappingInfo finalInfo = RequestMappingInfo.paths("/api").options(getBuilderConfiguration()).build().combine(baseInfo);
		if(method.isAnnotationPresent(AllowAnonymous.class) || method.getDeclaringClass().isAnnotationPresent(AllowAnonymous.class)) {
			finalInfo.getPathPatternsCondition().getPatterns().forEach(s -> {
				ANONYMOUS_URLS.add(s);
			});
		}
		return finalInfo;
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
			Set<PathPattern> basePatterns = baseInfo.getPathPatternsCondition().getPatterns();
			if (basePatterns.isEmpty()) {
				addPattern(baseInfo, method, handlerType);
			} else if (basePatterns.size() == 1 && basePatterns.iterator().next().equals("")) {
				try {
					patternsField.set(baseInfo.getPathPatternsCondition(), new LinkedHashSet<>());
				} catch (IllegalAccessException e) {
				}
				addPattern(baseInfo, method, handlerType);
			} else {
				RequestMappingInfo info = createRequestMappingInfo(method);
				if (info.getPathPatternsCondition().getPatterns().isEmpty()) {
					addPattern(baseInfo, method, handlerType);
				}
			}
			return baseInfo;
		}
	}

	private void addPattern(RequestMappingInfo baseInfo, Method method, Class<?> handlerType) {
		String secondPart = method.getName();
		PathPatternsRequestCondition patternsRequestCondition = baseInfo.getPathPatternsCondition();
		Set<PathPattern> basePatterns = patternsRequestCondition.getPatterns();
		Set<PathPattern> updatedPatterns = new LinkedHashSet<>();
		try {
			patternsField.set(patternsRequestCondition, updatedPatterns);
		} catch (IllegalAccessException e) {
		}
		if (basePatterns.isEmpty()) {
			String className = handlerType.getSimpleName();
			String firstPart = className.substring(0, 1).toLowerCase() + className.substring(1, className.length() - 10);
			updatedPatterns.add(getPatternParser().parse("/" + firstPart + "/" + secondPart));
		} else {
			for (PathPattern pattern : basePatterns) {
				updatedPatterns.add(pattern.combine(getPatternParser().parse(secondPart)));
			}
		}
	}

	private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
		return (requestMapping != null ? createRequestMappingInfo(requestMapping, null) : null);
	}
}