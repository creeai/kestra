package io.kestra.repository.postgres;

import io.kestra.core.models.namespaces.NamespacePluginDefaults;
import io.kestra.jdbc.repository.AbstractJdbcNamespacePluginDefaultsRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@PostgresRepositoryEnabled
public class PostgresNamespacePluginDefaultsRepository extends AbstractJdbcNamespacePluginDefaultsRepository {
    @Inject
    public PostgresNamespacePluginDefaultsRepository(@Named("namespaceplugindefaults") PostgresRepository<NamespacePluginDefaults> repository) {
        super(repository);
    }
}
