package io.kestra.repository.h2;

import io.kestra.core.models.auth.User;
import io.kestra.jdbc.repository.AbstractJdbcUserRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@H2RepositoryEnabled
public class H2UserRepository extends AbstractJdbcUserRepository {
    @Inject
    public H2UserRepository(@Named("users") H2Repository<User> repository) {
        super(repository);
    }
}
