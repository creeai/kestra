package io.kestra.repository.h2;

import io.kestra.core.models.namespaces.NamespaceVariable;
import io.kestra.jdbc.repository.AbstractJdbcNamespaceVariableRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@H2RepositoryEnabled
public class H2NamespaceVariableRepository extends AbstractJdbcNamespaceVariableRepository {
    @Inject
    public H2NamespaceVariableRepository(@Named("namespacevariables") H2Repository<NamespaceVariable> repository) {
        super(repository);
    }
}
