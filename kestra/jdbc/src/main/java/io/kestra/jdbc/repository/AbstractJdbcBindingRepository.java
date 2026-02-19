package io.kestra.jdbc.repository;

import io.kestra.core.models.auth.Binding;
import io.kestra.core.repositories.BindingRepositoryInterface;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Optional;

import static io.kestra.jdbc.repository.AbstractJdbcRepository.KEY_FIELD;

public abstract class AbstractJdbcBindingRepository extends AbstractJdbcCrudRepository<Binding> implements BindingRepositoryInterface {

    public AbstractJdbcBindingRepository(io.kestra.jdbc.AbstractJdbcRepository<Binding> jdbcRepository) {
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
    public Optional<Binding> findById(String id) {
        return findOne(DSL.trueCondition(), KEY_FIELD.eq(id));
    }

    @Override
    public List<Binding> findByUserId(String userId) {
        List<Binding> all = findAll(DSL.trueCondition());
        return all.stream().filter(b -> userId.equals(b.getUserId())).toList();
    }

    @Override
    public List<Binding> findByRoleId(String roleId) {
        List<Binding> all = findAll(DSL.trueCondition());
        return all.stream().filter(b -> roleId.equals(b.getRoleId())).toList();
    }

    @Override
    public List<Binding> findAll() {
        return findAll(DSL.trueCondition());
    }

    @Override
    public Binding save(Binding binding) {
        if (findById(binding.getId()).isEmpty()) {
            return create(binding);
        }
        return update(binding);
    }

    @Override
    public Binding delete(Binding binding) {
        Optional<Binding> found = findById(binding.getId());
        if (found.isEmpty()) {
            throw new IllegalStateException("Binding " + binding.getId() + " does not exist");
        }
        jdbcRepository.delete(binding);
        return binding;
    }
}
