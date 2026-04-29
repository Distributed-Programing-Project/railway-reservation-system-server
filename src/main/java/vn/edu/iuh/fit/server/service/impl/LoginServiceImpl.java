package vn.edu.iuh.fit.server.service.impl;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityManager;
import vn.edu.iuh.fit.common.dto.AccountDTO;
import vn.edu.iuh.fit.common.dto.LoginRequestDTO;
import vn.edu.iuh.fit.common.message.LoginMessages;
import vn.edu.iuh.fit.common.response.Response;
import vn.edu.iuh.fit.server.model.Account;
import vn.edu.iuh.fit.server.model.Role;
import vn.edu.iuh.fit.server.repository.AccountRepository;
import vn.edu.iuh.fit.server.repository.impl.AbstractGenericRepositoryImpl;
import vn.edu.iuh.fit.server.repository.impl.AccountRepositoryImpl;
import vn.edu.iuh.fit.server.service.LoginService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class LoginServiceImpl implements LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginServiceImpl.class);
    private final AccountRepository accountRepository = new AccountRepositoryImpl();

    @Override
    public Response login(LoginRequestDTO loginRequestDTO) {
        if (loginRequestDTO == null) {
            return Response.error(LoginMessages.LOGIN_DATA_REQUIRED);
        }

        String username = normalize(loginRequestDTO.getUsername());
        String password = loginRequestDTO.getPassword();
        if (username == null || password == null || password.isBlank()) {
            return Response.error(LoginMessages.USERNAME_PASSWORD_REQUIRED);
        }

        try {
            return AbstractGenericRepositoryImpl.readOnly(em -> {
                Account account = accountRepository.findByUsername(em, username);
                if (account == null) {
                    log.warn("Login failed: username not found={}", username);
                    return Response.error(LoginMessages.INVALID_CREDENTIALS);
                }

                if (!account.isActive()) {
                    log.warn("Login failed: account inactive username={}", username);
                    return Response.error(LoginMessages.ACCOUNT_INACTIVE);
                }
                if (!passwordMatches(password, account.getPassword())) {
                    log.warn("Login failed: invalid password username={}", username);
                    return Response.error(LoginMessages.INVALID_CREDENTIALS);
                }

                log.info("Login successful: username={}", username);
                return Response.success(LoginMessages.LOGIN_SUCCESS, AccountDTO.builder()
                        .id(account.getId())
                        .username(account.getUsername())
                        .active(account.isActive())
                        .roleCodes(resolveRoleCodes(em, account))
                        .build());
            });
        } catch (Exception e) {
            log.error("Login failed with system error: username={}", username, e);
            return Response.error(LoginMessages.SYSTEM_ERROR_PREFIX + e.getMessage());
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword.equals(storedPassword)) {
            return true;
        }
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (isBCryptHash(storedPassword)) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }
        return false;
    }

    private List<String> resolveRoleCodes(EntityManager em, Account account) {
        Set<String> roleCodes = account.getRoles() == null
                ? Set.of()
                : account.getRoles().stream()
                .map(Role::getCode)
                .filter(code -> code != null && !code.isBlank())
                .collect(Collectors.toSet());

        if (!roleCodes.isEmpty()) {
            return roleCodes.stream().sorted().toList();
        }

        Boolean isManager = em.createQuery(
                        "SELECT e.isManager FROM Employee e WHERE e.account.id = :accountId",
                        Boolean.class)
                .setParameter("accountId", account.getId())
                .getResultStream()
                .findFirst()
                .orElse(Boolean.FALSE);
        return List.of(Boolean.TRUE.equals(isManager) ? Role.ADMIN : Role.STAFF);
    }

    private boolean isBCryptHash(String value) {
        return value.startsWith("$2a$")
                || value.startsWith("$2b$")
                || value.startsWith("$2y$");
    }
}
