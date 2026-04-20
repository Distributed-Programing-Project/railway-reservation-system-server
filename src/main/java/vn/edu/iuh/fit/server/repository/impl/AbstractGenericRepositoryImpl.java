package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import vn.edu.iuh.fit.server.util.JPAUtils;

import java.util.function.Function;

public abstract class AbstractGenericRepositoryImpl<T, ID> {

    protected Class<T> entityClass;

    public AbstractGenericRepositoryImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Use for INSERT, UPDATE, DELETE operations that require a Transaction.
     */
    protected <R> R doInTransaction(Function<EntityManager, R> function) {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
            em = JPAUtils.getEntityManager();
            tx = em.getTransaction();
            tx.begin();
            R result = function.apply(em);
            tx.commit();
            return result;
        } catch (Exception ex) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException(ex);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Use for SELECT (search, filter) operations that do not require a Transaction.
     */
    protected <R> R doWithEntityManager(Function<EntityManager, R> function) {
        EntityManager em = null;
        try {
            em = JPAUtils.getEntityManager();
            return function.apply(em);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}
