package com.grameenphone.wipro.fmfs.cbp.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Hibernate configuration class for ui application database
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
		basePackages = "com.grameenphone.wipro.fmfs.cbp.repository.cbp",
		entityManagerFactoryRef = "cbpEntityManagerFactory",
		transactionManagerRef = "cbpTransactionManager"
)
public class CBPDbConfig {
	/**
	 * datasource bean
	 * @return a {@link HikariDataSource } instance
	 */
	@Bean(name = "cbpDataSource")
	@Primary
	@ConfigurationProperties(prefix = "cbp.datasource")
	public DataSource dataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).build();
	}

	/**
	 * entity manager factory bean
	 * @param builder
	 * @param dataSource
	 * @return
	 */
	@Bean(name = "cbpEntityManagerFactory")
	@Primary
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("cbpDataSource") DataSource dataSource) {
		return builder.dataSource(dataSource)
						.packages("com.grameenphone.wipro.fmfs.cbp.model.orm.cbp")
						.persistenceUnit("cbp")
						.build();
	}

	/**
	 * transaction manager bean
	 * @param entityManagerFactory
	 * @return
	 */
	@Bean(name = "cbpTransactionManager")
	@Primary
	public PlatformTransactionManager transactionManager(@Qualifier("cbpEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}