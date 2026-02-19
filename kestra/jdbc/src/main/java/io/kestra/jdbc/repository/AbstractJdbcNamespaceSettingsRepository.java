package io.kestra.jdbc.repository;

import io.kestra.core.models.namespaces.NamespaceSettings;
import io.kestra.core.repositories.NamespaceSettingsRepositoryInterface;
import io.kestra.core.utils.IdUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.Optional;

import static io.kestra.jdbc.repository.AbstractJdbcRepository.KEY_FIELD;

public abstract class AbstractJdbcNamespaceSettingsRepository extends AbstractJdbcCrudRepository<NamespaceSettings> implements NamespaceSettingsRepositoryInterface {

    public AbstractJdbcNamespaceSettingsRepository(io.kestra.jdbc.AbstractJdbcRepository<NamespaceSettings> jdbcRepository) {
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
    public Optional<NamespaceSettings> findByNamespace(String tenantId, String namespace) {
        String uid = IdUtils.fromParts(tenantId, namespace);
        return findOne(DSL.trueCondition(), KEY_FIELD.eq(uid));
    }

    @Override
    public NamespaceSettings save(NamespaceSettings settings) {
        Optional<NamespaceSettings> existing = findByNamespace(settings.getTenantId(), settings.getNamespace());
        if (existing.isPresent()) {
            return update(settings);
        }
        return create(settings);
    }

    @Override
    public void delete(String tenantId, String namespace) {
        findByNamespace(tenantId, namespace).ifPresent(jdbcRepository::delete);
    }
}
