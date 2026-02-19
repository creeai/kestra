package io.kestra.core.repositories;

import io.kestra.core.models.flows.PluginDefault;
import io.kestra.core.models.namespaces.NamespacePluginDefaults;

import java.util.List;
import java.util.Optional;

/**
 * Repository for namespace-level plugin defaults.
 */
public interface NamespacePluginDefaultsRepositoryInterface {
    Optional<NamespacePluginDefaults> findByNamespace(String tenantId, String namespace);

    default List<PluginDefault> getDefaultsByNamespace(String tenantId, String namespace) {
        return findByNamespace(tenantId, namespace)
            .map(NamespacePluginDefaults::getDefaults)
            .orElse(List.of());
    }

    NamespacePluginDefaults save(NamespacePluginDefaults defaults);

    void delete(String tenantId, String namespace);
}
