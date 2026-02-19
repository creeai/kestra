package io.kestra.webserver.services;

import io.kestra.core.models.auth.Binding;
import io.kestra.core.models.auth.Permission;
import io.kestra.core.models.auth.Action;
import io.kestra.core.models.auth.Role;
import io.kestra.core.models.auth.User;
import io.kestra.core.repositories.BindingRepositoryInterface;
import io.kestra.core.repositories.RoleRepositoryInterface;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Checks if a user has a given permission and action, optionally scoped by namespace.
 */
@Singleton
@Requires(beans = BindingRepositoryInterface.class)
public class RbacService {

    @Inject
    private BindingRepositoryInterface bindingRepository;

    @Inject
    private RoleRepositoryInterface roleRepository;

    /**
     * Check if the user has the given permission and action.
     * When namespace is provided, binding.namespace must be null (all namespaces) or a prefix of the requested namespace.
     */
    public boolean hasPermission(User user, Permission permission, Action action, String namespace) {
        if (user == null) {
            return false;
        }
        // Legacy synthetic user (from Basic Auth config when no DB users) has full access
        if (user.getId() != null && user.getId().startsWith("legacy-")) {
            return true;
        }
        List<Binding> bindings = bindingRepository.findByUserId(user.getId());
        for (Binding binding : bindings) {
            if (!namespaceMatches(binding.getNamespace(), namespace)) {
                continue;
            }
            Optional<Role> role = roleRepository.findById(binding.getRoleId());
            if (role.isPresent() && role.get().hasPermission(permission, action)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPermission(User user, Permission permission, Action action) {
        return hasPermission(user, permission, action, null);
    }

    private static boolean namespaceMatches(String bindingNamespace, String requestedNamespace) {
        if (StringUtils.isBlank(bindingNamespace)) {
            return true; // binding applies to all namespaces
        }
        if (StringUtils.isBlank(requestedNamespace)) {
            return true;
        }
        return requestedNamespace.equals(bindingNamespace) || requestedNamespace.startsWith(bindingNamespace + ".");
    }
}
