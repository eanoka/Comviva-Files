package com.grameenphone.wipro.fmfs.cbp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.grameenphone.wipro.constants.EventScopes;
import com.grameenphone.wipro.constants.Events;
import com.grameenphone.wipro.extensions.jackson.JacksonPropertyExcludableFilter;
import com.grameenphone.wipro.extensions.jackson.JsonPropertyExcludableMapperModule;
import com.grameenphone.wipro.extensions.spring.boot.beans.config.RequestProcessorUnifiedGateway;
import com.grameenphone.wipro.extensions.spring.boot.extra.ServletResourceCache;
import com.grameenphone.wipro.fmfs.cbp.filter.HeartBeatFilter;
import com.grameenphone.wipro.fmfs.cbp.filter.RequestPayloadLoggingFilter;
import com.grameenphone.wipro.utility.common.Event;
import com.grameenphone.wipro.utility.marshal.Json;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.io.IOException;
import java.util.List;

/**
 * Configuration class for spring mvc
 */
@Configuration
@EnableScheduling
public class MVCConfig extends WebMvcConfigurationSupport {
	boolean mappingResourceForRoot = true;

	/**
	 * Resource Url Mapping Handler added twice to handle root url mapped - as /** was getting priority over /.
	 * @param contentNegotiationManager
	 * @param conversionService
	 * @param resourceUrlProvider
	 * @return
	 */
	@Bean
	public SimpleUrlHandlerMapping rootUrlHandler(@Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager, @Qualifier("mvcConversionService") FormattingConversionService conversionService, @Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {
		return (SimpleUrlHandlerMapping) super.resourceHandlerMapping(contentNegotiationManager, conversionService, resourceUrlProvider);
	}

	/**
	 * @return a bean {@link RequestProcessorUnifiedGateway} for custom request mapping support
	 */
	protected RequestMappingHandlerAdapter createRequestMappingHandlerAdapter() {
		return new RequestProcessorUnifiedGateway(); // To Handle All Exception Raised from Different Services
	}

	/**
	 * @return a bean {@link RequestMappingHandlerMapping } for custom request mapping handle
	 */
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new com.grameenphone.wipro.extensions.spring.boot.beans.config.RequestMappingHandlerMapping(); //Overrided to prefix /api to every url
	}

	/**
	 * configures resource root directory
	 * @param registry
	 */
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
		if(mappingResourceForRoot) {
			registry.addResourceHandler("/").resourceChain(true, new ServletResourceCache("/ng/index.html"));
			mappingResourceForRoot = false;
		} else {
			registry.addResourceHandler("/static/**").addResourceLocations("/");
			registry.addResourceHandler("/**").addResourceLocations("/ng/");
		}
	}

	/**
	 * @return a bean which exposes request to current thread
	 */
	@Bean
	public RequestContextListener requestContextListener() {
		return new RequestContextListener();
	}

	@Override
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		ObjectMapper mapper = Json.mapper;
		JsonPropertyExcludableMapperModule module = new JsonPropertyExcludableMapperModule();
		mapper.registerModule(module);
		mapper.setFilterProvider(new SimpleFilterProvider().addFilter("default", new JacksonPropertyExcludableFilter(module)));
		converters.add(new MappingJackson2HttpMessageConverter(mapper));
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.defaultContentType(MediaType.APPLICATION_JSON);
	}

	@Bean
	public FilterRegistrationBean<HeartBeatFilter> heartBeatFilter() {
		FilterRegistrationBean<HeartBeatFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new HeartBeatFilter());
		registrationBean.setOrder(1);
		return registrationBean;
	}

	@Bean
	public FilterRegistrationBean<RequestPayloadLoggingFilter> loggingFilter(@Value("${request.log.payload.max.size}") Integer payLoadSize) {
		FilterRegistrationBean<RequestPayloadLoggingFilter> registrationBean = new FilterRegistrationBean<>();
		registrationBean.setFilter(new RequestPayloadLoggingFilter(payLoadSize));
		registrationBean.addUrlPatterns("/api/*");
		registrationBean.setOrder(2);
		return registrationBean;
	}

	@Bean
	public FilterRegistrationBean<OpenEntityManagerInViewFilter> cbpEntityManagerInView() {
		FilterRegistrationBean<OpenEntityManagerInViewFilter> registrationBean = new FilterRegistrationBean<>();
		OpenEntityManagerInViewFilter filter = new OpenEntityManagerInViewFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
				super.doFilterInternal(request, response, filterChain);
				Event.fire(Events.ENTITY_MANAGER_CLOSE, (Object)"cbp");
			}
		};
		filter.setPersistenceUnitName("cbp");
		registrationBean.setFilter(filter);
		registrationBean.addUrlPatterns("/api/*");
		registrationBean.setOrder(3);
		return registrationBean;
	}

	@Bean
	public FilterRegistrationBean<OpenEntityManagerInViewFilter> reportEntityManagerInView() {
		FilterRegistrationBean<OpenEntityManagerInViewFilter> registrationBean = new FilterRegistrationBean<>();
		OpenEntityManagerInViewFilter filter = new OpenEntityManagerInViewFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
				super.doFilterInternal(request, response, filterChain);
				Event.fire(Events.ENTITY_MANAGER_CLOSE, (Object)"report");
			}
		};
		filter.setPersistenceUnitName("report");
		registrationBean.setFilter(filter);
		registrationBean.addUrlPatterns("/api/*");
		registrationBean.setOrder(3);
		return registrationBean;
	}

	@Bean
	public FilterRegistrationBean<Filter> clearRequestScopedEvent() {
		FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
		Filter filter = (request, response, chain) -> {
			try {
				chain.doFilter(request, response);
			} finally {
				Event.off("*", EventScopes.REQUEST + "-" + Thread.currentThread().getId());
			}
		};
		registrationBean.setFilter(filter);
		registrationBean.setOrder(2);
		return registrationBean;
	}
}