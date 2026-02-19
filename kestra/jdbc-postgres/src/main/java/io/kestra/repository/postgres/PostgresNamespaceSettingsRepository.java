package io.kestra.repository.postgres;

import io.kestra.core.models.namespaces.NamespaceSettings;
import io.kestra.jdbc.repository.AbstractJdbcNamespaceSettingsRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@PostgresRepositoryEnabled
public class PostgresNamespaceSettingsRepository extends AbstractJdbcNamespaceSettingsRepository {
    @Inject
    public PostgresNamespaceSettingsRepository(@Named("namespacesettings") PostgresRepository<NamespaceSettings> repository) {
        super(repository);
    }
}
