package io.kestra.repository.h2;

import io.kestra.core.models.namespaces.NamespacePluginDefaults;
import io.kestra.jdbc.repository.AbstractJdbcNamespacePluginDefaultsRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@H2RepositoryEnabled
public class H2NamespacePluginDefaultsRepository extends AbstractJdbcNamespacePluginDefaultsRepository {
    @Inject
    public H2NamespacePluginDefaultsRepository(@Named("namespaceplugindefaults") H2Repository<NamespacePluginDefaults> repository) {
        super(repository);
    }
}
