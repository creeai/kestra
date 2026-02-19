package io.kestra.repository.postgres;

import io.kestra.core.models.auth.User;
import io.kestra.jdbc.repository.AbstractJdbcUserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@PostgresRepositoryEnabled
public class PostgresUserRepository extends AbstractJdbcUserRepository {
    @Inject
    public PostgresUserRepository(@Named("users") PostgresRepository<User> repository) {
        super(repository);
    }
}
