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
     * Execute a transactional operation (INSERT, UPDATE, DELETE).
     * Handles begin, commit, rollback on unchecked exception, and EM close.
     * @throws RuntimeException re-wraps any RuntimeException from the action after rollback
     */
    public static <R> R transactional(Function<EntityManager, R> action) {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
            em = JPAUtils.getEntityManager();
            tx = em.getTransaction();
            tx.begin();
            R result = action.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Execute a read-only operation with EntityManager.
     * Handles EM open/close; re-throws exception.
     */
    public static <R> R readOnly(Function<EntityManager, R> action) {
        EntityManager em = null;
        try {
            em = JPAUtils.getEntityManager();
            return action.apply(em);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Use for INSERT, UPDATE, DELETE in a new transaction.
     */
    protected <R> R doInTransaction(Function<EntityManager, R> function) {
        return transactional(function);
    }

    /**
     * Use for INSERT, UPDATE, DELETE with error handling.
     * @param onError called after rollback; returns the error result value
     */
    protected <R> R doInTransaction(Function<EntityManager, R> action, Function<Exception, R> onError) {
        EntityManager em = null;
        EntityTransaction tx = null;
        try {
            em = JPAUtils.getEntityManager();
            tx = em.getTransaction();
            tx.begin();
            R result = action.apply(em);
            tx.commit();
            return result;
        } catch (Exception ex) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            return onError.apply(ex);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Use for SELECT (search, filter) without transaction.
     */
    protected <R> R doWithEntityManager(Function<EntityManager, R> function) {
        return readOnly(function);
    }

    /**
     * Use for SELECT with error handling.
     * @param onError called when an exception occurs; returns the error result value
     */
    protected <R> R doWithEntityManager(Function<EntityManager, R> action, Function<Exception, R> onError) {
        EntityManager em = null;
        try {
            em = JPAUtils.getEntityManager();
            return action.apply(em);
        } catch (Exception e) {
            return onError.apply(e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }

    /**
     * Execute a read-only operation with EntityManager and error handling.
     * @param onError called when an exception occurs; returns the error result value
     */
    public static <R> R readOnly(Function<EntityManager, R> action, Function<Exception, R> onError) {
        EntityManager em = null;
        try {
            em = JPAUtils.getEntityManager();
            return action.apply(em);
        } catch (Exception e) {
            return onError.apply(e);
        } finally {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
    }
}
