package com.grameenphone.wipro.fmfs.mfs_communicator.controller;

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grameenphone.wipro.utility.TextView;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

@RestController
public class AdminController {
    @Autowired
    @Qualifier("flexmfsEntityManagerFactory")
    EntityManagerFactory entityManagerFactory;

    @Autowired
    @Qualifier("flexmfsDataSource")
    HikariDataSource flexmfsDatasource;

    @Autowired
    @Qualifier("mfsDataSource")
    HikariDataSource mfsDatasource;

    @Autowired
    ApplicationEventPublisher appEventPublisher;

    public String clearCache() {
        entityManagerFactory.getCache().evictAll();
        return "OK";
    }

    @RequestMapping("/")
    public String status() {
        return "OK";
    }

    @RequestMapping(value = "/dbpool/status")
    public TextView dbPoolStatus() {
        HikariPoolMXBean flexmfsPool = flexmfsDatasource.getHikariPoolMXBean();
        HikariPoolMXBean mfsPool = mfsDatasource.getHikariPoolMXBean();
        return new TextView("<html><h3>Flex MFS</h3>" +
                "<blockquote>" +
                "<strong>Total: </strong> " + flexmfsDatasource.getMaximumPoolSize() + "<br>" +
                "<strong>Active: </strong> " + flexmfsPool.getActiveConnections() + "<br>" +
                "<strong>Idle: </strong> " + flexmfsPool.getIdleConnections() + "<br>" +
                "<strong>Waiting: </strong> " + flexmfsPool.getThreadsAwaitingConnection() +
                "</blockquote>" +
                "<h3>MFS</h3>" +
                "<blockquote>" +
                "<strong>Total: </strong> " + mfsDatasource.getMaximumPoolSize() + "<br>" +
                "<strong>Active: </strong> " + mfsPool.getActiveConnections() + "<br>" +
                "<strong>Idle: </strong> " + mfsPool.getIdleConnections() + "<br>" +
                "<strong>Waiting: </strong> " + mfsPool.getThreadsAwaitingConnection() +
                "</blockquote></html>");
    }
}