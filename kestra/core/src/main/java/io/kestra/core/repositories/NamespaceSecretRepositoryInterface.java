package io.kestra.core.repositories;

import io.kestra.core.models.secret.NamespaceSecret;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for namespace-scoped secrets (stored encrypted).
 * When implemented (e.g. by JDBC module), allows managing secrets from the UI
 * without using environment variables.
 */
public interface NamespaceSecretRepositoryInterface {
    Optional<NamespaceSecret> findByNamespaceAndKey(String tenantId, String namespace, String key);

    NamespaceSecret save(NamespaceSecret secret);

    void delete(String tenantId, String namespace, String key);

    Set<String> listKeys(String tenantId, String namespace);

    /**
     * List all secrets for a namespace (meta only; caller must not expose valueEncrypted).
     */
    List<NamespaceSecret> list(String tenantId, String namespace);
}
