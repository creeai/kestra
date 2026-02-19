package io.kestra.repository.mysql;

import io.kestra.core.models.namespaces.NamespaceVariable;
import io.kestra.jdbc.repository.AbstractJdbcNamespaceVariableRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@MysqlRepositoryEnabled
public class MysqlNamespaceVariableRepository extends AbstractJdbcNamespaceVariableRepository {
    @Inject
    public MysqlNamespaceVariableRepository(@Named("namespacevariables") MysqlRepository<NamespaceVariable> repository) {
        super(repository);
    }
}
