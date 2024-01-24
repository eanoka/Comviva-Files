package com.grameenphone.wipro.fmfs.cbp.filter;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.grameenphone.wipro.utility.common.PathMatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.grameenphone.wipro.utility.common.StringUtil;
import com.sun.management.OperatingSystemMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

@Component
@WebFilter(filterName = "healthLogFilter", urlPatterns = "/*")
public class HealthLogFilter implements Filter {
    private Logger logger = LoggerFactory.getLogger("HealthLog");

    private volatile static int hitCount = 0;
    private volatile static Map<Integer, Integer> statusMap = new ConcurrentHashMap<>();
    private volatile static float avgResponseTime;
    private volatile static int maxResponseTime;
    private PathMatcher staticUrls = new PathMatcher("/static/**");

    @Value("#{cbpDataSource}")
    HikariDataSource cbpDatasource;

    @Value("#{reportDataSource}")
    HikariDataSource reportDatasource;

    @Value("${health-log.scheduler.interval:10m}")
    private String schedulerInterval;

    @Value("#{T(com.grameenphone.wipro.utility.common.StringUtil).toSecond('${health-log.scheduler.interval:10m}')}")
    private long schedulerIntervalInSecond;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        boolean nonStaticUrl = !staticUrls.matches((HttpServletRequest) request);
        try {
            if (nonStaticUrl) {
                hitCount++;
            }
        } catch (Throwable p) {}
        try {
            chain.doFilter(request, response);
        } finally {
            if(nonStaticUrl) {
                int responseTime = (int) (System.currentTimeMillis() - startTime);
                if (responseTime > maxResponseTime) {
                    maxResponseTime = responseTime;
                }
                avgResponseTime = ((avgResponseTime * (hitCount - 1)) + responseTime) / hitCount;
                int responseStatus = ((HttpServletResponse) response).getStatus();
                Integer statusCount = statusMap.get(responseStatus);
                if (statusCount == null) {
                    statusCount = 1;
                } else {
                    statusCount++;
                }
                statusMap.put(responseStatus, statusCount);
            }
        }
    }

    @Override
    public void destroy() {
    }

    @PostConstruct
    private void convertPropsFromOrdinal() {
        //Dynamically adding properties to convert value given in properties file from ordinal format (3h 5m) to numeric format
        System.setProperty("communicator.health-log.scheduler.interval", "" + schedulerIntervalInSecond * 1000);
    }

    @Scheduled(initialDelayString = "${communicator.health-log.scheduler.interval}", fixedDelayString = "${communicator.health-log.scheduler.interval}")
    public void execute() {
        HikariPoolMXBean mysqlMxBean = cbpDatasource.getHikariPoolMXBean();
        logger.debug("MYSQL DB HEALTH - Total: " + cbpDatasource.getMaximumPoolSize() + " Active: " + mysqlMxBean.getActiveConnections() + " Idle: " + mysqlMxBean.getIdleConnections() + " Waiting: " + mysqlMxBean.getThreadsAwaitingConnection());

        HikariPoolMXBean oracleMxBean = reportDatasource.getHikariPoolMXBean();
        logger.debug("Oracle DB HEALTH - Total: " + reportDatasource.getMaximumPoolSize() + " Active: " + oracleMxBean.getActiveConnections() + " Idle: " + oracleMxBean.getIdleConnections() + " Waiting: " + oracleMxBean.getThreadsAwaitingConnection());

        logger.debug("Application HEALTH (within last " + schedulerInterval + ") - HitCount: " + hitCount + " TPS: " + (hitCount/(float)schedulerIntervalInSecond) + " Response Time(avg): " + avgResponseTime + "ms Response Time(max): " + maxResponseTime + "ms");
        logger.debug("Counts Per Status: " + statusMap);

        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        logger.debug("VM HEALTH - Memory Free To Use: " + StringUtil.toGbMbKb(osBean.getFreeMemorySize()) + " CPU Used: " + osBean.getCpuLoad() * 100 + "%");

        hitCount = 0;
        avgResponseTime = 0;
        maxResponseTime = 0;
        statusMap.clear();
    }
}