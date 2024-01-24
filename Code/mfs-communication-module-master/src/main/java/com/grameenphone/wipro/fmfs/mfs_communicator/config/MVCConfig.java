package com.grameenphone.wipro.fmfs.mfs_communicator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import com.grameenphone.wipro.fmfs.mfs_communicator.interceptor.RequestInterceptor;
import com.grameenphone.wipro.utility.marshal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Configuration class for spring mvc
 */
@Configuration
@EnableScheduling
@EnableAsync
public class MVCConfig extends WebMvcConfigurationSupport {
	/**
	 * @return a bean {@link com.grameenphone.wipro.spring.boot.beans.config.RequestMappingHandlerAdapter} for custom request mapping support
	 */
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		return new com.grameenphone.wipro.spring.boot.beans.config.RequestMappingHandlerAdapter();
	}

	/**
	 * @return a bean {@link RequestMappingHandlerMapping } for custom request mapping handle
	 */
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new com.grameenphone.wipro.spring.boot.beans.config.RequestMappingHandlerMapping();
	}

	/**
	 * configures resource root directory
	 * @param registry
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**/*.*").addResourceLocations("/");
	}

	/**
	 * @return a bean which exposes request to current thread
	 */
	@Bean
	public RequestContextListener requestContextListener() {
		return new RequestContextListener();
	}

	/**
	 * Sets servlet context root path in a static variable to make it retrievable from any location
	 * @param servletContext
	 */
	public void setServletContext(ServletContext servletContext) {
		super.setServletContext(servletContext);
		Application.basePath = servletContext.getContextPath();
		if(Application.basePath.equals("/")) {
			Application.basePath = "";
		}
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.defaultContentType(MediaType.APPLICATION_JSON);
	}

	@Override
	/**
	 * To handle all the exception by filter
	 */
	protected void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
		exceptionResolvers.add((request, response, handler, ex) -> null);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new RequestInterceptor());
	}

	@Override
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		ObjectMapper mapper = Json.mapper;
		converters.add(new MappingJackson2HttpMessageConverter(mapper));
	}

	@Bean
	public FilterRegistrationBean<OncePerRequestFilter> whitelistFilter(@Value("${whitelist.urls}") List<String> whiteListedUrls) {
		FilterRegistrationBean<OncePerRequestFilter> registrationBean = new FilterRegistrationBean<>();
		Logger filterLogger = LoggerFactory.getLogger(MVCConfig.class);
		registrationBean.setFilter(new OncePerRequestFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
				String sourceIp = request.getHeader("X-Forwarded-For");
				if(sourceIp == null) {
					sourceIp = request.getRemoteAddr();
				}
				if(whiteListedUrls.contains(sourceIp)) {
					filterChain.doFilter(request, response);
				} else {
					filterLogger.debug(sourceIp + " is Blacklisted");
					response.getOutputStream().write("Blacklisted".getBytes());
				}
			}
		});
		registrationBean.setOrder(1);
		return registrationBean;
	}
}