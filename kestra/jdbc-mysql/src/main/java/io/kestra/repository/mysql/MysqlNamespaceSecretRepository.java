package io.kestra.repository.mysql;

import io.kestra.core.models.secret.NamespaceSecret;
import io.kestra.jdbc.repository.AbstractJdbcNamespaceSecretRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@MysqlRepositoryEnabled
public class MysqlNamespaceSecretRepository extends AbstractJdbcNamespaceSecretRepository {
    @Inject
    public MysqlNamespaceSecretRepository(@Named("namespacesecrets") MysqlRepository<NamespaceSecret> repository) {
        super(repository);
    }
}
