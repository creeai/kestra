package io.kestra.jdbc.repository;

import io.kestra.core.models.secret.NamespaceSecret;
import io.kestra.core.repositories.NamespaceSecretRepositoryInterface;
import io.kestra.core.utils.IdUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.kestra.jdbc.repository.AbstractJdbcRepository.KEY_FIELD;

public abstract class AbstractJdbcNamespaceSecretRepository extends AbstractJdbcCrudRepository<NamespaceSecret> implements NamespaceSecretRepositoryInterface {

    public AbstractJdbcNamespaceSecretRepository(io.kestra.jdbc.AbstractJdbcRepository<NamespaceSecret> jdbcRepository) {
        super(jdbcRepository);
    }

    @Override
    protected Condition defaultFilter(String tenantId) {
        return DSL.trueCondition();
    }

    @Override
    protected Condition defaultFilter() {
        return DSL.trueCondition();
    }

    @Override
    public Optional<NamespaceSecret> findByNamespaceAndKey(String tenantId, String namespace, String key) {
        String uid = IdUtils.fromParts(tenantId, namespace, key.toUpperCase());
        return findOne(DSL.trueCondition(), KEY_FIELD.eq(uid));
    }

    @Override
    public NamespaceSecret save(NamespaceSecret secret) {
        Optional<NamespaceSecret> existing = findByNamespaceAndKey(secret.getTenantId(), secret.getNamespace(), secret.getKey());
        if (existing.isPresent()) {
            return update(secret);
        }
        return create(secret);
    }

    @Override
    public void delete(String tenantId, String namespace, String key) {
        findByNamespaceAndKey(tenantId, namespace, key).ifPresent(jdbcRepository::delete);
    }

    @Override
    public Set<String> listKeys(String tenantId, String namespace) {
        String prefix = IdUtils.fromParts(tenantId, namespace, "") + "%";
        return find(DSL.trueCondition(), KEY_FIELD.like(prefix)).stream()
            .map(NamespaceSecret::getKey)
            .collect(Collectors.toSet());
    }

    @Override
    public List<NamespaceSecret> list(String tenantId, String namespace) {
        String prefix = IdUtils.fromParts(tenantId, namespace, "") + "%";
        return find(DSL.trueCondition(), KEY_FIELD.like(prefix)).stream().toList();
    }
}
