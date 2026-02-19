package io.kestra.jdbc.repository;

import io.kestra.core.models.namespaces.NamespaceVariable;
import io.kestra.core.repositories.NamespaceVariableRepositoryInterface;
import io.kestra.core.utils.IdUtils;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.kestra.jdbc.repository.AbstractJdbcRepository.KEY_FIELD;

public abstract class AbstractJdbcNamespaceVariableRepository extends AbstractJdbcCrudRepository<NamespaceVariable> implements NamespaceVariableRepositoryInterface {

    public AbstractJdbcNamespaceVariableRepository(io.kestra.jdbc.AbstractJdbcRepository<NamespaceVariable> jdbcRepository) {
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
    public Optional<NamespaceVariable> findByNamespaceAndKey(String tenantId, String namespace, String key) {
        String uid = IdUtils.fromParts(tenantId, namespace, key);
        return findOne(DSL.trueCondition(), KEY_FIELD.eq(uid));
    }

    @Override
    public Map<String, String> getVariablesByNamespace(String tenantId, String namespace) {
        return list(tenantId, namespace).stream()
            .collect(Collectors.toMap(NamespaceVariable::getKey, NamespaceVariable::getValue));
    }

    @Override
    public NamespaceVariable save(NamespaceVariable variable) {
        Optional<NamespaceVariable> existing = findByNamespaceAndKey(variable.getTenantId(), variable.getNamespace(), variable.getKey());
        if (existing.isPresent()) {
            return update(variable);
        }
        return create(variable);
    }

    @Override
    public void delete(String tenantId, String namespace, String key) {
        findByNamespaceAndKey(tenantId, namespace, key).ifPresent(jdbcRepository::delete);
    }

    @Override
    public List<NamespaceVariable> list(String tenantId, String namespace) {
        String prefix = IdUtils.fromParts(tenantId, namespace, "") + "%";
        return find(DSL.trueCondition(), KEY_FIELD.like(prefix)).stream().toList();
    }
}
