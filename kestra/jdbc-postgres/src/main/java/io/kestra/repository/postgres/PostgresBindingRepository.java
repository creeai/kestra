package io.kestra.repository.postgres;

import io.kestra.core.models.auth.Binding;
import io.kestra.jdbc.repository.AbstractJdbcBindingRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@PostgresRepositoryEnabled
public class PostgresBindingRepository extends AbstractJdbcBindingRepository {
    @Inject
    public PostgresBindingRepository(@Named("bindings") PostgresRepository<Binding> repository) {
        super(repository);
    }
}
