package io.kestra.repository.h2;

import io.kestra.core.models.auth.Role;
import io.kestra.jdbc.repository.AbstractJdbcRoleRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@H2RepositoryEnabled
public class H2RoleRepository extends AbstractJdbcRoleRepository {
    @Inject
    public H2RoleRepository(@Named("roles") H2Repository<Role> repository) {
        super(repository);
    }
}
