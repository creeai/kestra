package io.kestra.core.services;

import io.kestra.core.exceptions.ResourceAccessDeniedException;
import io.kestra.core.models.namespaces.NamespaceSettings;
import io.kestra.core.repositories.FlowRepositoryInterface;
import io.kestra.core.repositories.NamespaceSettingsRepositoryInterface;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of {@link NamespaceService}.
 */
@Singleton
public class DefaultNamespaceService implements NamespaceService {

    private final Optional<FlowRepositoryInterface> flowRepository;
    private final Optional<NamespaceSettingsRepositoryInterface> namespaceSettingsRepository;
    private final boolean requireExistingNamespace;

    @Inject
    public DefaultNamespaceService(
        Optional<FlowRepositoryInterface> flowRepository,
        Optional<NamespaceSettingsRepositoryInterface> namespaceSettingsRepository,
        @Value("${kestra.require-existing-namespace:false}") boolean requireExistingNamespace
    ) {
        this.flowRepository = flowRepository;
        this.namespaceSettingsRepository = namespaceSettingsRepository;
        this.requireExistingNamespace = requireExistingNamespace;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNamespaceExists(String tenant, String namespace) {
        Objects.requireNonNull(namespace, "namespace cannot be null");
        return flowRepository.map(repository -> repository.isNamespaceExists(tenant, namespace)).orElse(false);
    }

    /**
     * {@inheritDoc}
     * When {@code kestra.require-existing-namespace} is true, returns true if the namespace does not yet exist
     * (so that flow creation will be rejected for placeholder namespaces).
     */
    @Override
    public boolean requireExistingNamespace(String tenant, String namespace) {
        return requireExistingNamespace && !isNamespaceExists(tenant, namespace);
    }

    /**
     * {@inheritDoc}
     * When namespace settings exist and allowedNamespaces is set, only those namespaces may access;
     * otherwise all namespaces are allowed.
     */
    @Override
    public boolean isAllowedNamespace(String tenant, String namespace, String fromTenant, String fromNamespace) {
        if (namespaceSettingsRepository.isEmpty()) {
            return true;
        }
        Optional<NamespaceSettings> settings = namespaceSettingsRepository.get().findByNamespace(tenant, namespace);
        if (settings.isEmpty()) {
            return true;
        }
        List<String> allowed = settings.get().getAllowedNamespaces();
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        return allowed.contains(fromNamespace);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkAllowedNamespace(String tenant, String namespace, String fromTenant, String fromNamespace) {
        if (!isAllowedNamespace(tenant, namespace, fromTenant, fromNamespace)) {
            throw new ResourceAccessDeniedException("Namespace " + namespace + " is not allowed.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkAllowedAllNamespaces(String tenant, String fromTenant, String fromNamespace) {
        if (!areAllowedAllNamespaces(tenant, fromTenant, fromNamespace)) {
            throw new ResourceAccessDeniedException("All namespaces are not allowed, you should either filter on a namespace or configure all namespaces to allow your namespace.");
        }
    }
}
