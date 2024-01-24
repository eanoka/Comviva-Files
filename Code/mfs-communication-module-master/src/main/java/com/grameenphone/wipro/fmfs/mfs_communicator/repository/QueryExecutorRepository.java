package com.grameenphone.wipro.fmfs.mfs_communicator.repository;

import com.grameenphone.wipro.utility.KV;
import com.grameenphone.wipro.utility.common.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class QueryExecutorRepository {
    protected static final Logger logger = LoggerFactory.getLogger(QueryExecutorRepository.class);

    protected EntityManagerFactory entityManagerFactory;

    protected EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    public <T> List<T> getResultList(String query, Class<T> rowClass, Object... params) {
        return executeQueryWithLog(query, (entityManager) -> {
            Query jpaQuery = entityManager.createNativeQuery(query, rowClass.getName());
            for (int h = 1; h <= params.length; h++) {
                jpaQuery.setParameter(h, params[h - 1]);
            }
            return jpaQuery.getResultList();
        });
    }

    public <T> List<T> getResultList(String query, Class<T> rowClass, KV<String, Object>... params) {
        return executeQueryWithLog(query, (entityManager) -> {
            Query jpaQuery = entityManager.createNativeQuery(query, rowClass.getName());
            Arrays.stream(params).forEach(p -> jpaQuery.setParameter(p.key, p.value));
            return jpaQuery.getResultList();
        });
    }

    public <T> T getSingleResult(String query, Class<T> rowClass, Object... params) {
        List<T> results = getResultList(query, rowClass, params);
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

    public <T> T getSingleResult(String query, Class<T> rowClass, KV<String, Object>... params) {
        List<T> results = getResultList(query, rowClass, params);
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

    @Transactional
    public void executeUpdate(String query, KV<String, Object>... params) {
        executeQueryWithLog(query, (entityManager) -> {
            entityManager.joinTransaction();
            Query jpaQuery = entityManager.createNativeQuery(query);
            Arrays.stream(params).forEach(p -> jpaQuery.setParameter(p.key, p.value));
            jpaQuery.executeUpdate();
            return null;
        });
    }

    private <T> T executeQueryWithLog(String query, Function<EntityManager, T> func) {
        Instant start = Instant.now();
        EntityManager entityManager = null;
        try {
            entityManager = getEntityManager();
            return func.apply(entityManager);
        } finally {
            logger.debug("Took time for query execution: " + StringUtil.milliTohms(ChronoUnit.MILLIS.between(start, Instant.now())) + " (" + (query.length() > 50 ? query.substring(0, 50) + "..." : query) + ")");
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }
}
