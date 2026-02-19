package io.kestra.core.repositories;

import io.kestra.core.models.namespaces.NamespaceSettings;

import java.util.Optional;

/**
 * Repository for namespace-level settings (description, allowed namespaces).
 */
public interface NamespaceSettingsRepositoryInterface {
    Optional<NamespaceSettings> findByNamespace(String tenantId, String namespace);

    NamespaceSettings save(NamespaceSettings settings);

    void delete(String tenantId, String namespace);
}
