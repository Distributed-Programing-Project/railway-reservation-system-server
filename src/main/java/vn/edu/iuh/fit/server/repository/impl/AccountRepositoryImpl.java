package vn.edu.iuh.fit.server.repository.impl;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Account;
import vn.edu.iuh.fit.server.repository.AccountRepository;

public class AccountRepositoryImpl extends AbstractGenericRepositoryImpl<Account, String>
        implements AccountRepository {

    public AccountRepositoryImpl() {
        super(Account.class);
    }

    @Override
    public Account findByUsername(EntityManager em, String username) {
        return em.createQuery("SELECT DISTINCT a FROM Account a LEFT JOIN FETCH a.roles WHERE a.username = :username", Account.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
