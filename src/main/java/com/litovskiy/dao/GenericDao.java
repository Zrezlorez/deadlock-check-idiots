package com.litovskiy.dao;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import java.util.List;

public class GenericDao<T> extends BaseDao {
    private final Class<T> type;

    public GenericDao(Class<T> type) {
        this.type = type;
    }

    public T find(long id) {
        return execute(s -> s.get(type, id));
    }

    public T findByField(String fieldName, Object value) {
        return execute(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(type);
            Root<T> root = cq.from(type);

            cq.select(root)
                .where(cb.equal(root.get(fieldName), value));

            return session.createQuery(cq)
                .setMaxResults(1)
                .uniqueResult();
        });
    }

    public List<T> findAll(int page, int size,
                           String sortBy,
                           boolean asc) {
        return execute(session -> {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<T> cq = cb.createQuery(type);
            Root<T> root = cq.from(type);

            Order order = asc
                ? cb.asc(root.get(sortBy))
                : cb.desc(root.get(sortBy));

            cq.select(root).orderBy(order);

            return session.createQuery(cq)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
        });
    }

    public void save(T entity) {
        executeVoid(s -> s.merge(entity));
    }
}
