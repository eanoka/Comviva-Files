package com.grameenphone.wipro.fmfs.mfs_communicator.filter;

import com.grameenphone.wipro.utility.common.StringUtil;
import com.sun.management.OperatingSystemMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@WebFilter(filterName = "healthLogFilter", urlPatterns = "/*")
public class HealthLogFilter implements Filter {
    private Logger logger = LoggerFactory.getLogger("HealthLog");

    private volatile static int hitCount = 0;
    private volatile static Map<Integer, Integer> statusMap = new ConcurrentHashMap<>();
    private volatile static float avgResponseTime;
    private volatile static int maxResponseTime;

    @Value("#{flexmfsDataSource}")
    HikariDataSource mysqlDatasource;

    @Value("#{mfsDataSource}")
    HikariDataSource oracleDatasource;

    @Value("#{mfsReportDataSource}")
    HikariDataSource oracleReportDatasource;

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
        try {
            chain.doFilter(request, response);
        } finally {
            int responseTime = (int)(System.currentTimeMillis() - startTime);
            hitCount++;
            if(responseTime > maxResponseTime) {
                maxResponseTime = responseTime;
            }
            avgResponseTime = ((avgResponseTime * (hitCount -1)) + responseTime) / hitCount;
            int responseStatus = ((HttpServletResponse)response).getStatus();
            Integer statusCount = statusMap.get(responseStatus);
            if(statusCount == null) {
                statusCount = 1;
            } else {
                statusCount++;
            }
            statusMap.put(responseStatus, statusCount);
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
        HikariPoolMXBean mysqlMxBean = mysqlDatasource.getHikariPoolMXBean();
        logger.debug("MYSQL DB HEALTH - Total: " + mysqlDatasource.getMaximumPoolSize() + " Active: " + mysqlMxBean.getActiveConnections() + " Idle: " + mysqlMxBean.getIdleConnections() + " Waiting: " + mysqlMxBean.getThreadsAwaitingConnection());

        HikariPoolMXBean oracleMxBean = oracleDatasource.getHikariPoolMXBean();
        logger.debug("Oracle DB HEALTH - Total: " + oracleDatasource.getMaximumPoolSize() + " Active: " + oracleMxBean.getActiveConnections() + " Idle: " + oracleMxBean.getIdleConnections() + " Waiting: " + oracleMxBean.getThreadsAwaitingConnection());

        HikariPoolMXBean oracleReportMxBean = oracleReportDatasource.getHikariPoolMXBean();
        logger.debug("Oracle Report DB HEALTH - Total: " + oracleReportDatasource.getMaximumPoolSize() + " Active: " + oracleReportMxBean.getActiveConnections() + " Idle: " + oracleReportMxBean.getIdleConnections() + " Waiting: " + oracleReportMxBean.getThreadsAwaitingConnection());

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
