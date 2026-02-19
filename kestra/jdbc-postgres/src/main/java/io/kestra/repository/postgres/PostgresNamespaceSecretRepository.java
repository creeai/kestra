package io.kestra.repository.postgres;

import io.kestra.core.models.secret.NamespaceSecret;
import io.kestra.jdbc.repository.AbstractJdbcNamespaceSecretRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@PostgresRepositoryEnabled
public class PostgresNamespaceSecretRepository extends AbstractJdbcNamespaceSecretRepository {
    @Inject
    public PostgresNamespaceSecretRepository(@Named("namespacesecrets") PostgresRepository<NamespaceSecret> repository) {
        super(repository);
    }
}
