package io.kestra.repository.postgres;

import io.kestra.core.models.auth.Role;
import io.kestra.jdbc.repository.AbstractJdbcRoleRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@PostgresRepositoryEnabled
public class PostgresRoleRepository extends AbstractJdbcRoleRepository {
    @Inject
    public PostgresRoleRepository(@Named("roles") PostgresRepository<Role> repository) {
        super(repository);
    }
}
