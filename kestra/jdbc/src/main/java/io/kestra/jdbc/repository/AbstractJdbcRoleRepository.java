package io.kestra.jdbc.repository;

import io.kestra.core.models.auth.Role;
import io.kestra.core.repositories.RoleRepositoryInterface;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Optional;

import static io.kestra.jdbc.repository.AbstractJdbcRepository.KEY_FIELD;

public abstract class AbstractJdbcRoleRepository extends AbstractJdbcCrudRepository<Role> implements RoleRepositoryInterface {

    public AbstractJdbcRoleRepository(io.kestra.jdbc.AbstractJdbcRepository<Role> jdbcRepository) {
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
    public Optional<Role> findById(String id) {
        return findOne(DSL.trueCondition(), KEY_FIELD.eq(id));
    }

    @Override
    public List<Role> findAll() {
        return findAll(DSL.trueCondition());
    }

    @Override
    public Role save(Role role) {
        if (findById(role.getId()).isEmpty()) {
            return create(role);
        }
        return update(role);
    }

    @Override
    public Role delete(Role role) {
        Optional<Role> found = findById(role.getId());
        if (found.isEmpty()) {
            throw new IllegalStateException("Role " + role.getId() + " does not exist");
        }
        jdbcRepository.delete(role);
        return role;
    }
}
