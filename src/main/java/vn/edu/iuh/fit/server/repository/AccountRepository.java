package vn.edu.iuh.fit.server.repository;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.server.model.Account;

public interface AccountRepository {

    Account findByUsername(EntityManager em, String username);
}
