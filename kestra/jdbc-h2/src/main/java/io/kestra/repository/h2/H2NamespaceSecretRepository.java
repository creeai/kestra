package io.kestra.repository.h2;

import io.kestra.core.models.secret.NamespaceSecret;
import io.kestra.jdbc.repository.AbstractJdbcNamespaceSecretRepository;
import io.micronaut.context.ApplicationContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@H2RepositoryEnabled
public class H2NamespaceSecretRepository extends AbstractJdbcNamespaceSecretRepository {
    @Inject
    public H2NamespaceSecretRepository(@Named("namespacesecrets") H2Repository<NamespaceSecret> repository) {
        super(repository);
    }
}
