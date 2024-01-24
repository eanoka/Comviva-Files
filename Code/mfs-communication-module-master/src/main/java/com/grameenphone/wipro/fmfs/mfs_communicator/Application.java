package com.grameenphone.wipro.fmfs.mfs_communicator;

import com.grameenphone.wipro.spring.boot.extra.ExternalPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.context.WebApplicationContext;

@SpringBootApplication
public class Application extends SpringBootServletInitializer {
	private final static Logger log = LoggerFactory.getLogger(Application.class);

	public static ApplicationContext context;
	public static StandardEnvironment environment;
	public static String basePath;

	//region FOR JAR STARTUP
	public static void main(String[] args) {
		log.info("Server starting as embedded");
		SpringApplication application = new SpringApplication(Application.class);
		addInitializer(application);
		application.run(args);
	}
	//endregion

	//region FOR WAR STARTUP
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		builder.sources(this.getClass());
		return super.configure(builder);
	}

	@Override
	protected WebApplicationContext run(SpringApplication application) {
		addInitializer(application);
		return super.run(application);
	}
	//endregion

	private static void addInitializer(SpringApplication application) {
		application.addInitializers((applicationContext) -> {
			Application.context = applicationContext;
			Application.environment = (StandardEnvironment) applicationContext.getEnvironment();
			Application.environment.getPropertySources().addLast(new ExternalPropertySource());
		});
	}
}