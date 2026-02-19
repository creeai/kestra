package io.kestra.repository.mysql;

import io.kestra.core.models.namespaces.NamespacePluginDefaults;
import io.kestra.jdbc.repository.AbstractJdbcNamespacePluginDefaultsRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@MysqlRepositoryEnabled
public class MysqlNamespacePluginDefaultsRepository extends AbstractJdbcNamespacePluginDefaultsRepository {
    @Inject
    public MysqlNamespacePluginDefaultsRepository(@Named("namespaceplugindefaults") MysqlRepository<NamespacePluginDefaults> repository) {
        super(repository);
    }
}
