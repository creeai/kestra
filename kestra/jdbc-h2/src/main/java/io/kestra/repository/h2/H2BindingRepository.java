package io.kestra.repository.h2;

import io.kestra.core.models.auth.Binding;
import io.kestra.jdbc.repository.AbstractJdbcBindingRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@H2RepositoryEnabled
public class H2BindingRepository extends AbstractJdbcBindingRepository {
    @Inject
    public H2BindingRepository(@Named("bindings") H2Repository<Binding> repository) {
        super(repository);
    }
}
