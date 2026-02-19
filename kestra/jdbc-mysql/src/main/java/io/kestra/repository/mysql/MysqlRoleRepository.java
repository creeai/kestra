package io.kestra.repository.mysql;

import io.kestra.core.models.auth.Role;
import io.kestra.jdbc.repository.AbstractJdbcRoleRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@MysqlRepositoryEnabled
public class MysqlRoleRepository extends AbstractJdbcRoleRepository {
    @Inject
    public MysqlRoleRepository(@Named("roles") MysqlRepository<Role> repository) {
        super(repository);
    }
}
