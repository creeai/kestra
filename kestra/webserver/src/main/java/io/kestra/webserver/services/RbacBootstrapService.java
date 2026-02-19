package io.kestra.webserver.services;

import io.kestra.core.models.auth.Action;
import io.kestra.core.models.auth.Binding;
import io.kestra.core.models.auth.Permission;
import io.kestra.core.models.auth.Role;
import io.kestra.core.models.auth.User;
import io.kestra.core.repositories.BindingRepositoryInterface;
import io.kestra.core.repositories.RoleRepositoryInterface;
import io.kestra.core.repositories.UserRepositoryInterface;
import io.kestra.core.utils.AuthUtils;
import io.kestra.core.utils.IdUtils;
import io.micronaut.context.annotation.Requires;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * On startup: creates Admin role (all permissions) if missing, and first user from Basic Auth config if no users exist.
 */
@Singleton
@Requires(property = "kestra.server-type", pattern = "(WEBSERVER|STANDALONE)")
@Requires(property = "micronaut.security.enabled", notEquals = "true")
@Requires(beans = UserRepositoryInterface.class)
public class RbacBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(RbacBootstrapService.class);
    public static final String ADMIN_ROLE_ID = "admin";

    private final Optional<UserRepositoryInterface> userRepository;

    private final Optional<RoleRepositoryInterface> roleRepository;
    private final Optional<BindingRepositoryInterface> bindingRepository;
    private final BasicAuthService basicAuthService;

    @Inject
    public RbacBootstrapService(
        Optional<UserRepositoryInterface> userRepository,
        Optional<RoleRepositoryInterface> roleRepository,
        Optional<BindingRepositoryInterface> bindingRepository,
        BasicAuthService basicAuthService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.bindingRepository = bindingRepository;
        this.basicAuthService = basicAuthService;
    }

    @EventListener
    public void onServerStartup(ServerStartupEvent event) {
        if (roleRepository.isEmpty() || bindingRepository.isEmpty()) {
            return;
        }
        ensureAdminRole();
        if (userRepository.isPresent() && userRepository.get().findAll().isEmpty()) {
            ensureFirstUserFromConfig();
        }
    }

    private void ensureAdminRole() {
        if (roleRepository.get().findById(ADMIN_ROLE_ID).isPresent()) {
            return;
        }
        Set<String> allPermissions = EnumSet.allOf(Permission.class).stream()
            .flatMap(p -> EnumSet.allOf(Action.class).stream().map(a -> p.name() + ":" + a.name()))
            .collect(Collectors.toSet());
        Role admin = Role.builder()
            .id(ADMIN_ROLE_ID)
            .name("Admin")
            .description("Full access to all resources")
            .permissions(allPermissions)
            .build();
        roleRepository.get().save(admin);
        log.info("Created Admin role");
    }

    private void ensureFirstUserFromConfig() {
        var config = basicAuthService.configuration();
        if (config.credentials() == null || StringUtils.isBlank(config.credentials().getUsername()) || StringUtils.isBlank(config.credentials().getPassword())) {
            return;
        }
        String username = config.credentials().getUsername();
        if (userRepository.get().findByUsername(username).isPresent()) {
            return;
        }
        String salt = AuthUtils.generateSalt();
        String passwordHash = AuthUtils.encodePassword(salt, config.credentials().getPassword());
        User user = User.builder()
            .id(IdUtils.create())
            .username(username)
            .passwordHash(passwordHash)
            .salt(salt)
            .disabled(false)
            .createdAt(Instant.now())
            .build();
        userRepository.get().save(user);
        Binding binding = Binding.builder()
            .id(IdUtils.create())
            .roleId(ADMIN_ROLE_ID)
            .userId(user.getId())
            .groupId(null)
            .namespace(null)
            .build();
        bindingRepository.get().save(binding);
        log.info("Created first user '{}' with Admin role from Basic Auth config", username);
    }
}
