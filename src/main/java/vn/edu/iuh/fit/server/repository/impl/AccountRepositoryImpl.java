package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Account;
import vn.edu.iuh.fit.server.repository.AccountRepository;

import java.util.Locale;

public class AccountRepositoryImpl extends AbstractGenericRepositoryImpl<Account, String>
        implements AccountRepository {

    public AccountRepositoryImpl() {
        super(Account.class);
    }

    @Override
    public Account findByUsername(EntityManager em, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String normalizedLogin = username.trim().toLowerCase(Locale.ROOT);
        return em.createQuery("""
                SELECT a
                FROM Account a
                WHERE LOWER(a.username) = :login
                   OR EXISTS (
                        SELECT 1
                        FROM Employee e
                        WHERE e.account = a
                          AND LOWER(e.email) = :login
                   )
                """, Account.class)
                .setParameter("login", normalizedLogin)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
