package io.kestra.webserver.services;

import io.kestra.core.models.audit.AuditLog;
import io.kestra.core.models.auth.User;
import io.kestra.core.repositories.AuditLogRepositoryInterface;
import io.kestra.core.utils.IdUtils;
import io.kestra.webserver.filter.AuthenticationFilter;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Instant;
import java.util.Map;

/**
 * Service to record audit log entries. Controllers should call this after create/update/delete operations.
 */
@Singleton
@Requires(beans = AuditLogRepositoryInterface.class)
public class AuditService {

    @Inject
    private AuditLogRepositoryInterface auditLogRepository;

    public void log(
        User actor,
        AuditLog.AuditAction action,
        String resourceType,
        String resourceId,
        String namespace,
        Map<String, Object> details
    ) {
        AuditLog.AuditActorType actorType = actor != null && actor.getId() != null && actor.getId().startsWith("sa-")
            ? AuditLog.AuditActorType.SERVICE_ACCOUNT
            : AuditLog.AuditActorType.USER;
        String actorId = actor != null ? actor.getId() : "anonymous";

        AuditLog log = AuditLog.builder()
            .id(IdUtils.create())
            .timestamp(Instant.now())
            .actorId(actorId)
            .actorType(actorType)
            .action(action)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .namespace(namespace)
            .details(details)
            .build();
        auditLogRepository.save(log);
    }
}
