package io.kestra.jdbc.repository;

import io.kestra.core.models.namespaces.NamespacePluginDefaults;
import io.kestra.core.repositories.NamespacePluginDefaultsRepositoryInterface;
import io.kestra.core.utils.IdUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.Optional;

import static io.kestra.jdbc.repository.AbstractJdbcRepository.KEY_FIELD;

public abstract class AbstractJdbcNamespacePluginDefaultsRepository
    extends AbstractJdbcCrudRepository<NamespacePluginDefaults>
    implements NamespacePluginDefaultsRepositoryInterface {

    public AbstractJdbcNamespacePluginDefaultsRepository(
        io.kestra.jdbc.AbstractJdbcRepository<NamespacePluginDefaults> jdbcRepository
    ) {
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
    public Optional<NamespacePluginDefaults> findByNamespace(String tenantId, String namespace) {
        String uid = IdUtils.fromParts(tenantId, namespace);
        return findOne(DSL.trueCondition(), KEY_FIELD.eq(uid));
    }

    @Override
    public NamespacePluginDefaults save(NamespacePluginDefaults defaults) {
        Optional<NamespacePluginDefaults> existing = findByNamespace(defaults.getTenantId(), defaults.getNamespace());
        if (existing.isPresent()) {
            return update(defaults);
        }
        return create(defaults);
    }

    @Override
    public void delete(String tenantId, String namespace) {
        findByNamespace(tenantId, namespace).ifPresent(jdbcRepository::delete);
    }
}
