package io.kestra.jdbc.repository;

import io.kestra.core.models.auth.User;
import io.kestra.core.repositories.UserRepositoryInterface;
import org.jooq.Condition;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Optional;

import static io.kestra.jdbc.repository.AbstractJdbcRepository.KEY_FIELD;

public abstract class AbstractJdbcUserRepository extends AbstractJdbcCrudRepository<User> implements UserRepositoryInterface {

    public AbstractJdbcUserRepository(io.kestra.jdbc.AbstractJdbcRepository<User> jdbcRepository) {
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
    public Optional<User> findById(String id) {
        return findOne(DSL.trueCondition(), KEY_FIELD.eq(id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        List<User> all = findAll(DSL.trueCondition());
        return all.stream().filter(u -> username.equals(u.getUsername())).findFirst();
    }

    @Override
    public List<User> findAll() {
        return findAll(DSL.trueCondition());
    }

    @Override
    public User save(User user) {
        if (findById(user.getId()).isEmpty()) {
            return create(user);
        }
        return update(user);
    }

    @Override
    public User delete(User user) {
        Optional<User> found = findById(user.getId());
        if (found.isEmpty()) {
            throw new IllegalStateException("User " + user.getId() + " does not exist");
        }
        jdbcRepository.delete(user);
        return user;
    }
}
