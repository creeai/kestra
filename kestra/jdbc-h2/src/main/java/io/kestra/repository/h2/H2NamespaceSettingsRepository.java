package io.kestra.repository.h2;

import io.kestra.core.models.namespaces.NamespaceSettings;
import io.kestra.jdbc.repository.AbstractJdbcNamespaceSettingsRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@H2RepositoryEnabled
public class H2NamespaceSettingsRepository extends AbstractJdbcNamespaceSettingsRepository {
    @Inject
    public H2NamespaceSettingsRepository(@Named("namespacesettings") H2Repository<NamespaceSettings> repository) {
        super(repository);
    }
}
