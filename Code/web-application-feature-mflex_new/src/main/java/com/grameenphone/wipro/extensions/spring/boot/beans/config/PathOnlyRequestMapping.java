package com.grameenphone.wipro.extensions.spring.boot.beans.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;

public class PathOnlyRequestMapping implements RequestMapping {
	String[] path;

	public PathOnlyRequestMapping(String... path) {
		this.path = path;
	}

	@Override
	public boolean equals(Object obj) {
		return false;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return "/" + StringUtils.join(path, "/");
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return RequestMapping.class;
	}

	@Override
	public String name() {
		return "";
	}

	@Override
	public String[] value() {
		return path();
	}

	@Override
	public String[] path() {
		return path;
	}

	@Override
	public RequestMethod[] method() {
		return new RequestMethod[0];
	}

	@Override
	public String[] params() {
		return new String[0];
	}

	@Override
	public String[] headers() {
		return new String[0];
	}

	@Override
	public String[] consumes() {
		return new String[0];
	}

	@Override
	public String[] produces() {
		return new String[] {MediaType.APPLICATION_JSON_VALUE};
	}
}