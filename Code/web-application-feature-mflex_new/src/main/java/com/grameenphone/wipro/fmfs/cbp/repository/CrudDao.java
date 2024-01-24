package com.grameenphone.wipro.fmfs.cbp.repository;

import com.grameenphone.wipro.exception.AppRuntimeException;
import com.grameenphone.wipro.fmfs.cbp.Application;
import com.grameenphone.wipro.utility.KV;
import com.grameenphone.wipro.utility.orm.WhereBuilder;
import org.hibernate.Session;
import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generic dao for dealing with data and object
 * @param <T> Class to be passed
 */
@Repository
@Scope("prototype")
public class CrudDao<T> {
	@PersistenceContext(unitName = "cbp")
	private EntityManager cbpEntityManager;

	@PersistenceContext(unitName = "report")
	private EntityManager reportEntityManager;

	protected EntityManager entityManager;

	private Class<T> clazz;

	private EntityManager getEntityManager() {
		if(entityManager == null) {
			throw new AppRuntimeException("Generic clazz not defined");
		}
		return entityManager;
	}

	protected void setClazz(Class<T> clazz) {
		this.clazz = clazz;
		if (clazz.getPackageName().contains("orm.cbp")) {
			entityManager = cbpEntityManager;
		} else {
			entityManager = reportEntityManager;
		}
	}

	public static <T> CrudDao<T> get(Class<T> clazz) {
		CrudDao dao = Application.context.getBean(CrudDao.class);
		dao.setClazz(clazz);
		return dao;
	}

	public static class ClosableCrudDao<U> extends CrudDao<U> implements Closeable {
		public ClosableCrudDao(Class<U> clazz) {
			setClazz(clazz);
			if (clazz.getPackageName().contains("orm.cbp")) {
				entityManager = Application.context.getBean("cbpEntityManagerFactory", EntityManagerFactory.class).createEntityManager();
			} else {
				entityManager = Application.context.getBean("reportEntityManagerFactory", EntityManagerFactory.class).createEntityManager();
			}
		}

		@Override
		public void close() {
			entityManager.close();
		}
	}

	public static <T> ClosableCrudDao<T> getInNewSession(Class<T> clazz) {
		return new ClosableCrudDao<>(clazz);
	}

	public Class<T> getClazz() {
		return this.clazz;
	}

	public T findOne(long id) {
		return getEntityManager().find(clazz, id);
	}

	public T save(T entity) {
		getEntityManager().persist(entity);
		getEntityManager().flush();
		return entity;
	}

	public Iterable<T> saveAll(Iterable<T> entities) {
		List<T> savedEntities = new ArrayList<>();
		entities.forEach(en -> savedEntities.add(save(en)));
		return savedEntities;
	}

	public boolean existsById(Long aLong) {
		return false;
	}

	public List<T> findAll() {
		return getEntityManager().createQuery("from " + clazz.getName()).getResultList();
	}

	public List<T> findAll(long offset, int limit) {
		Query query = getEntityManager().createQuery("from " + clazz.getName());
		query.setMaxResults(limit);
		query.setFirstResult((int)offset);
		return query.getResultList();
	}

	public Iterable<T> findAllById(Iterable<Long> longs) {
		return null;
	}

	public long count() {
		return (Long)getEntityManager().createQuery("select count(T) from " + clazz.getName() + " T").getSingleResult();
	}

	public long count(CriteriaQuery criteriaQuery) {
		return (Long)getEntityManager().createQuery(criteriaQuery).getSingleResult();
	}

	private long count(Map<String, Object> criterias, boolean anyMatch) {
		CriteriaBuilder builder = getBuilder();
		CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
		Root<T> root = criteriaQuery.from(clazz);
		criteriaQuery.select(builder.count(root));
		Expression<Boolean> whereCondition;
		if(criterias != null) {
			if (criterias.size() > 1) {
				List<Predicate> predicates = new ArrayList<>();
				criterias.forEach((k, v) -> {
					predicates.add(builder.equal(root.get(k), v));
				});
				Predicate[] matches = predicates.toArray(new Predicate[0]);
				whereCondition = anyMatch ? builder.or(matches) : builder.and(matches);
			} else {
				Map.Entry<String, Object> entry = criterias.entrySet().iterator().next();
				whereCondition = builder.equal(root.get(entry.getKey()), entry.getValue());
			}
			criteriaQuery.where(whereCondition);
		}
		return getEntityManager().createQuery(criteriaQuery).getSingleResult();
	}

	public long countByAllMatch(Map<String, Object> criterias) {
		return count(criterias, false);
	}

	public long countByAnyMatch(Map<String, Object> criterias) {
		return count(criterias, true);
	}

	private CriteriaBuilder getBuilder() {
		return getEntityManager().getCriteriaBuilder();
	}

	public Object getOne(CriteriaQuery criteriaQuery) {
		Query query = getEntityManager().createQuery(criteriaQuery);
		try {
			return query.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}
	}

	public List getAll(CriteriaQuery criteriaQuery, Long offset, Integer limit) {
		Query query = getEntityManager().createQuery(criteriaQuery);
		if (limit != null && limit != -1) {
			query.setMaxResults(limit);
			query.setFirstResult(offset.intValue());
		}
		return query.getResultList();
	}

	public WhereBuilder<T, ?> query() {
		CriteriaBuilder builder = getBuilder();
		return new WhereBuilder<>(this, builder);
	}

	public List<Object> executeQuery(String query, long offset, int perPage, KV<String, Object>... params) {
		return executeQuery(query, offset, perPage, Arrays.stream(params).collect(Collectors.toMap(kv -> kv.key, kv -> kv.value)));
	}

	public List<Object> executeQuery(String query, long offset, int perPage, Map<String, Object> params) {
		Query query1 = getEntityManager().createQuery(query);
		params.forEach((k, v) -> query1.setParameter(k, v));
		if(perPage != -1) {
			query1.setFirstResult((int)offset);
			query1.setMaxResults(perPage);
		}
		return query1.getResultList();
	}

	private CriteriaQuery<T> prepareQuery(Map<String, Object> criterias, boolean allMatch) {
		CriteriaBuilder builder = getBuilder();
		CriteriaQuery<T> criteriaQuery = builder.createQuery(clazz);
		Root<T> root = criteriaQuery.from(clazz);
		criteriaQuery.select(root);
		Expression<Boolean> whereCondition;
		if(criterias != null) {
			if (criterias.size() > 1) {
				List<Predicate> predicates = new ArrayList<>();
				criterias.forEach((k, v) -> {
					if(v == null) {
						predicates.add(builder.isNull(root.get(k)));
					} else {
						predicates.add(builder.equal(root.get(k), v));
					}
				});
				Predicate[] matches = predicates.toArray(new Predicate[0]);
				whereCondition = allMatch ? builder.and(matches) : builder.or(matches);
			} else {
				Map.Entry<String, Object> entry = criterias.entrySet().iterator().next();
				whereCondition = builder.equal(root.get(entry.getKey()), entry.getValue());
			}
			criteriaQuery.where(whereCondition);
		}
		return criteriaQuery;
	}

	private CriteriaQuery<T> prepareQueryAllMatch(Map<String, Object> criterias) {
		return prepareQuery(criterias, true);
	}

	private CriteriaQuery<T> prepareQueryAnyMatch(Map<String, Object> criterias) {
		return prepareQuery(criterias, false);
	}

	public T findOneByAllMatches(Map<String, Object> criterias) {
		return (T) getOne(prepareQueryAllMatch(criterias));
	}

	public T findOneByAllMatches(KV<String, Object>... criteria) {
		Map<String, Object> criterias = new HashMap<>();
		Arrays.stream(criteria).forEach(kv -> criterias.put(kv.key, kv.value));
		return (T) getOne(prepareQueryAllMatch(criterias));
	}

	public List<T> findAllByAllMatches(Map<String, Object> criterias) {
		return findAllByAllMatches(criterias, 0L, -1, null, null);
	}

	public List<T> findAllByAllMatches(Map<String, Object> criterias, Long offset, Integer limit, String order, String dir) {
		CriteriaQuery<T> criteriaQuery = prepareQueryAllMatch(criterias);
		CriteriaBuilder builder = getBuilder();
		if(order != null) {
			Root<T> root = (Root<T>)criteriaQuery.getRoots().iterator().next();
			Expression field = root.get(order);
			criteriaQuery.orderBy("asc".equals(dir) ? builder.asc(field) : builder.desc(field));
		}
		return getAll(criteriaQuery, offset, limit);
	}

	public List<T> findAllByAnyMatches(Map<String, Object> criterias, Long offset, Integer limit, String order, String dir) {
		CriteriaQuery<T> criteriaQuery = prepareQueryAnyMatch(criterias);
		CriteriaBuilder builder = getBuilder();
		if(order != null) {
			Root<T> root = (Root<T>)criteriaQuery.getSelection();
			Expression field = root.get(order);
			criteriaQuery.orderBy("asc".equals(dir) ? builder.asc(field) : builder.desc(field));
		}
		return getAll(criteriaQuery, offset, limit);
	}

	public T update(T entity) {
		return getEntityManager().merge(entity);
	}

	public void delete(T entity) {
		getEntityManager().remove(entity);
	}

	public void deleteById(long entityId) {
		T entity = findOne(entityId);
		delete(entity);
	}

	public T proxy(Long id) {
		return getEntityManager().getReference(clazz, id);
	}

	@Transactional(readOnly = true)
	public T refresh(T entity) {
		getEntityManager().refresh(entity);
		return entity;
	}

	@Transactional(readOnly = true)
	public T attach(T entity) {
		return getEntityManager().merge(entity);
	}

	@Transactional(readOnly = true)
	public void setInSession(LazyInitializer initializer) {
		initializer.setSession(getEntityManager().unwrap(SharedSessionContractImplementor.class));
	}

	public void initialize(AbstractPersistentCollection collection) {
		SharedSessionContractImplementor session = getEntityManager().unwrap(SharedSessionContractImplementor.class);
		collection.setCurrentSession(session);
		boolean isJTA = session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();
		if ( !isJTA ) {
			session.beginTransaction();
		}
		try {
			session.getPersistenceContextInternal().addUninitializedDetachedCollection(session.getSessionFactory().getMappingMetamodel().findCollectionDescriptor(collection.getRole()), collection);
			session.initializeCollection(collection, false);
		} finally {
			if ( !isJTA ) {
				session.getTransaction().rollback();
			}
		}
	}
}