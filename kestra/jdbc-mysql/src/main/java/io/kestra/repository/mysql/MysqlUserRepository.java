package io.kestra.repository.mysql;

import io.kestra.core.models.auth.User;
import io.kestra.jdbc.repository.AbstractJdbcUserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@MysqlRepositoryEnabled
public class MysqlUserRepository extends AbstractJdbcUserRepository {
    @Inject
    public MysqlUserRepository(@Named("users") MysqlRepository<User> repository) {
        super(repository);
    }
}
