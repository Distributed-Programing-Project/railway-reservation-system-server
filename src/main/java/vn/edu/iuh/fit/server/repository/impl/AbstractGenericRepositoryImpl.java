package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import vn.edu.iuh.fit.server.util.JPAUtils;

import java.util.function.Function;

// Lớp cơ sở abstract cho các repository, hỗ trợ quản lý EntityManager và transaction
public abstract class AbstractGenericRepositoryImpl<T, ID> {

    // Lưu class của entity để các repository con sử dụng (ví dụ: User.class)
    protected Class<T> entityClass;

    // Constructor nhận vào class của entity tương ứng
    public AbstractGenericRepositoryImpl(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    // Thực thi hành động trong transaction: begin -> action -> commit, nếu lỗi thì rollback
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

    // Thực thi hành động chỉ đọc (không có transaction), chỉ cần mở và đóng EntityManager
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

    // [Deprecated] Wrapper cho transactional, sử dụng để tương thích ngược với code cũ
    @Deprecated
    protected <R> R doInTransaction(Function<EntityManager, R> function) {
        return transactional(function);
    }

    // [Deprecated] Tương tự transactional nhưng gọi onError khi có lỗi thay vì throw
    @Deprecated
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

    // [Deprecated] Wrapper cho readOnly, sử dụng để tương thích ngược với code cũ
    @Deprecated
    protected <R> R doWithEntityManager(Function<EntityManager, R> function) {
        return readOnly(function);
    }

    // [Deprecated] Wrapper cho readOnly(action, onError)
    @Deprecated
    protected <R> R doWithEntityManager(Function<EntityManager, R> action, Function<Exception, R> onError) {
        return readOnly(action, onError);
    }

    // Tương tự readOnly nhưng có xử lý lỗi riêng qua onError, không throw exception
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
