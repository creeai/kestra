package io.kestra.repository.postgres;

import io.kestra.core.models.namespaces.NamespaceVariable;
import io.kestra.jdbc.repository.AbstractJdbcNamespaceVariableRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@PostgresRepositoryEnabled
public class PostgresNamespaceVariableRepository extends AbstractJdbcNamespaceVariableRepository {
    @Inject
    public PostgresNamespaceVariableRepository(@Named("namespacevariables") PostgresRepository<NamespaceVariable> repository) {
        super(repository);
    }
}
