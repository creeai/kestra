package io.kestra.repository.mysql;

import io.kestra.core.models.namespaces.NamespaceSettings;
import io.kestra.jdbc.repository.AbstractJdbcNamespaceSettingsRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@MysqlRepositoryEnabled
public class MysqlNamespaceSettingsRepository extends AbstractJdbcNamespaceSettingsRepository {
    @Inject
    public MysqlNamespaceSettingsRepository(@Named("namespacesettings") MysqlRepository<NamespaceSettings> repository) {
        super(repository);
    }
}
