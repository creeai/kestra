package io.kestra.core.repositories;

import io.kestra.core.models.audit.AuditLog;
import io.kestra.core.repositories.ArrayListTotal;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepositoryInterface {

    AuditLog save(AuditLog auditLog);

    ArrayListTotal<AuditLog> find(
        Integer page,
        Integer size,
        String sort,
        Instant from,
        Instant to,
        String actorId,
        String resourceType,
        String resourceId,
        String namespace
    );
}
