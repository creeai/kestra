package io.kestra.core.repositories;

import io.kestra.core.models.namespaces.NamespaceVariable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for namespace-level variables (exposed as {{ namespace.key }} in expressions).
 */
public interface NamespaceVariableRepositoryInterface {
    Optional<NamespaceVariable> findByNamespaceAndKey(String tenantId, String namespace, String key);

    Map<String, String> getVariablesByNamespace(String tenantId, String namespace);

    NamespaceVariable save(NamespaceVariable variable);

    void delete(String tenantId, String namespace, String key);

    List<NamespaceVariable> list(String tenantId, String namespace);
}
