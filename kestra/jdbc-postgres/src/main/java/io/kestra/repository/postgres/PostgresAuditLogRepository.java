package io.kestra.repository.postgres;

import io.kestra.core.models.audit.AuditLog;
import io.kestra.jdbc.repository.AbstractJdbcAuditLogRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
@PostgresRepositoryEnabled
public class PostgresAuditLogRepository extends AbstractJdbcAuditLogRepository {

    @Inject
    public PostgresAuditLogRepository(@Named("auditlogs") PostgresRepository<AuditLog> repository) {
        super(repository);
    }
}
