package io.kestra.repository.mysql;

import io.kestra.core.models.auth.Binding;
import io.kestra.jdbc.repository.AbstractJdbcBindingRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@MysqlRepositoryEnabled
public class MysqlBindingRepository extends AbstractJdbcBindingRepository {
    @Inject
    public MysqlBindingRepository(@Named("bindings") MysqlRepository<Binding> repository) {
        super(repository);
    }
}
