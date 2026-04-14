package vn.edu.iuh.fit.server.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAUtils {

    private static final String PERSISTENCE_UNIT_NAME = "mariadb-pu";
    private static EntityManagerFactory factory;

    static {
        try {
            factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize EntityManagerFactory: " + e.getMessage());
        }
    }

    public static EntityManagerFactory getFactory() {
        return factory;
    }

    public static EntityManager getEntityManager() {
        return factory.createEntityManager();
    }
}
