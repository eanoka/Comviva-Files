package com.grameenphone.wipro.fmfs.cbp.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Hibernate configuration class for report portal database
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
		basePackages = "com.grameenphone.wipro.fmfs.cbp.repository.report",
		entityManagerFactoryRef = "reportEntityManagerFactory",
		transactionManagerRef = "reportTransactionManager"
)
public class ReportDbConfig {
	/**
	 * datasource bean
	 * @return a {@link HikariDataSource } instance
	 */
	@Bean(name = "reportDataSource")
	@ConfigurationProperties(prefix = "report.datasource")
	public DataSource dataSource() {
		return DataSourceBuilder.create().type(HikariDataSource.class).build();
	}

	/**
	 * entity manager factory bean
	 * @param builder
	 * @param dataSource
	 * @return
	 */
	@Bean(name = "reportEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("reportDataSource") DataSource dataSource) {
		return builder.dataSource(dataSource)
				.packages("com.grameenphone.wipro.fmfs.cbp.model.orm.report")
				.persistenceUnit("report")
				.build();
	}

	/**
	 * transaction manager bean
	 * @param entityManagerFactory
	 * @return
	 */
	@Bean(name = "reportTransactionManager")
	public PlatformTransactionManager transactionManager(@Qualifier("reportEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}