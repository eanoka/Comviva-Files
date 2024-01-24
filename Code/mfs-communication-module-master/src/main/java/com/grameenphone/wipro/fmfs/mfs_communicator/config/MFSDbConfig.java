package com.grameenphone.wipro.fmfs.mfs_communicator.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.dialect.Oracle10gDialect;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.LinkedHashMap;

/**
 * Hibernate configuration class for MFS database
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.grameenphone.wipro.fmfs.mfs_communicator.repository.mfs",
        entityManagerFactoryRef = "mfsEntityManagerFactory", transactionManagerRef = "mfsTransactionManager")
public class MFSDbConfig {
	@Value("${mfs.datasource.default.schema}")
	String defaultSchema;

    /**
     * datasource bean
     * 
     * @return a {@link HikariDataSource } instance
     */
    @Bean(name = "mfsDataSource")
    @Primary
    @ConfigurationProperties(prefix = "mfs.datasource")
    public DataSource dataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class).build();
        HikariConfigMXBean config = dataSource.getHikariConfigMXBean();
        if(Application.environment.getProperty("mfs.datasource.maximumPoolSize") == null) {
            config.setMaximumPoolSize(10);
        }
        if(Application.environment.getProperty("mfs.datasource.minimumIdle") == null) {
            config.setMinimumIdle(0);
        }
        if(Application.environment.getProperty("mfs.datasource.connectionTimeout") == null) {
            config.setConnectionTimeout(60000);
        }
        if(Application.environment.getProperty("mfs.datasource.idleTimeout") == null) {
            config.setIdleTimeout(300000);
        }
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return dataSource;
    }

    /**
     * entity manager factory bean
     * 
     * @param builder
     * @param dataSource
     * @return
     */
    @Bean(name = "mfsEntityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("mfsDataSource") DataSource dataSource) {
        return builder.dataSource(dataSource).properties(new LinkedHashMap<>() {{
            put("hibernate.dialect", Oracle10gDialect.class.getName());
            put("hibernate.default_schema", defaultSchema);
        }}).packages("com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.mfs").persistenceUnit("mfs").build();
    }

    /**
     * transaction manager bean
     * 
     * @param entityManagerFactory
     * @return
     */
    @Bean(name = "mfsTransactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("mfsEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
