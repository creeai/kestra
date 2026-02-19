package io.kestra.webserver.services;

import io.kestra.core.models.auth.User;
import io.kestra.core.repositories.UserRepositoryInterface;
import io.kestra.core.utils.AuthUtils;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Optional;

/**
 * Resolves the current User from Basic Auth credentials.
 * Tries UserRepository first (DB users), then falls back to BasicAuthService (legacy config).
 */
@Singleton
@Requires(property = "kestra.server-type", pattern = "(WEBSERVER|STANDALONE)")
@Requires(property = "micronaut.security.enabled", notEquals = "true")
public class AuthUserService {

    @Inject
    private BasicAuthService basicAuthService;

    private final Optional<UserRepositoryInterface> userRepository;

    @Inject
    public AuthUserService(Optional<UserRepositoryInterface> userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Resolve User from username and password.
     * First tries UserRepository; if not found or wrong password, tries BasicAuthService (legacy).
     * When legacy auth succeeds, returns a synthetic User so RBAC can apply (e.g. default admin).
     */
    public Optional<User> resolveUser(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return Optional.empty();
        }
        // Try DB users first
        if (userRepository.isPresent()) {
            Optional<User> dbUser = userRepository.get().findByUsername(username);
            if (dbUser.isPresent()) {
                User u = dbUser.get();
                if (u.isDisabled()) {
                    return Optional.empty();
                }
                String hash = AuthUtils.encodePassword(u.getSalt() != null ? u.getSalt() : "", password);
                if (hash.equals(u.getPasswordHash())) {
                    return Optional.of(u);
                }
                return Optional.empty();
            }
        }
        // Fallback to legacy Basic Auth (single credential from config)
        var config = basicAuthService.configuration();
        if (config.credentials() != null
            && username.equals(config.credentials().getUsername())
            && AuthUtils.encodePassword(config.credentials().getSalt(), password).equals(config.credentials().getPassword())) {
            // Synthetic user for legacy auth so RBAC can treat as admin when no DB users yet
            User synthetic = User.builder()
                .id("legacy-" + username)
                .username(username)
                .passwordHash("")
                .salt("")
                .disabled(false)
                .createdAt(Instant.now())
                .build();
            return Optional.of(synthetic);
        }
        return Optional.empty();
    }

    public boolean hasUsersInDb() {
        return userRepository.map(repo -> !repo.findAll().isEmpty()).orElse(false);
    }
}
