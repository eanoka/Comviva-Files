package com.grameenphone.wipro.fmfs.mfs_communicator.config;

import com.grameenphone.wipro.fmfs.mfs_communicator.Application;
import com.grameenphone.wipro.spring.orm.NamingStrategy;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.cache.ehcache.internal.SingletonEhcacheRegionFactory;
import org.hibernate.dialect.MySQL57Dialect;
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

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.LinkedHashMap;

/**
 * Hibernate configuration class for flex mfs application database
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs",
        entityManagerFactoryRef = "flexmfsEntityManagerFactory",
        transactionManagerRef = "flexmfsTransactionManager")
public class FlexMFSDbConfig {
    /**
     * datasource bean
     *
     * @return a {@link HikariDataSource } instance
     */
    @Bean(name = "flexmfsDataSource")
    @ConfigurationProperties(prefix = "flexmfs.datasource")
    public DataSource dataSource() {
        HikariDataSource dataSource = DataSourceBuilder.create().type(HikariDataSource.class).build();
        HikariConfigMXBean config = dataSource.getHikariConfigMXBean();
        if(Application.environment.getProperty("flexmfs.datasource.maximumPoolSize") == null) {
            config.setMaximumPoolSize(10);
        }
        if(Application.environment.getProperty("flexmfs.datasource.minimumIdle") == null) {
            config.setMinimumIdle(0);
        }
        if(Application.environment.getProperty("flexmfs.datasource.connectionTimeout") == null) {
            config.setConnectionTimeout(60000);
        }
        if(Application.environment.getProperty("flexmfs.datasource.idleTimeout") == null) {
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
    @Bean(name = "flexmfsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier("flexmfsDataSource") DataSource dataSource) {
        return builder.dataSource(dataSource).properties(new LinkedHashMap<>() {{
            put("hibernate.dialect", MySQL57Dialect.class.getName());
            put("hibernate.implicit_naming_strategy", NamingStrategy.class.getName());
            put("hibernate.cache.use_query_cache", "true");
            put("hibernate.cache.region.factory_class", SingletonEhcacheRegionFactory.class.getName());
            put("hibernate.hbm2ddl.auto","update");
        }}).packages("com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs").persistenceUnit("flexmfs").build();
    }

    /**
     * transaction manager bean
     *
     * @param entityManagerFactory
     * @return
     */
    @Bean(name = "flexmfsTransactionManager")
    public PlatformTransactionManager transactionManager(@Qualifier("flexmfsEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}