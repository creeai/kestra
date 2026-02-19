package io.kestra.webserver.controllers.api;

import io.kestra.core.models.auth.Action;
import io.kestra.core.models.auth.Permission;
import io.kestra.core.models.audit.AuditLog;
import io.kestra.core.repositories.ArrayListTotal;
import io.kestra.core.repositories.AuditLogRepositoryInterface;
import io.kestra.webserver.filter.AuthenticationFilter;
import io.kestra.webserver.responses.PagedResults;
import io.kestra.webserver.services.RbacService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.context.annotation.Requires;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.inject.Inject;

import java.time.Instant;

@Controller("/api/v1/audit-logs")
@ExecuteOn(TaskExecutors.IO)
@Requires(beans = AuditLogRepositoryInterface.class)
public class AuditLogController {

    @Inject
    private AuditLogRepositoryInterface auditLogRepository;

    @Inject
    private RbacService rbacService;

    @Get
    @Operation(tags = {"Audit"}, summary = "List audit logs")
    public HttpResponse<PagedResults<AuditLog>> list(
        HttpRequest<?> request,
        @QueryValue(defaultValue = "1") Integer page,
        @QueryValue(defaultValue = "25") Integer size,
        @QueryValue(defaultValue = "timestamp:desc") String sort,
        @QueryValue Instant from,
        @QueryValue Instant to,
        @QueryValue String actorId,
        @QueryValue String resourceType,
        @QueryValue String resourceId,
        @QueryValue String namespace
    ) {
        io.kestra.core.models.auth.User current = request.getAttributes()
            .get(AuthenticationFilter.REQUEST_ATTRIBUTE_USER, io.kestra.core.models.auth.User.class).orElse(null);
        if (current == null || !rbacService.hasPermission(current, Permission.AUDIT, Action.READ)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        ArrayListTotal<AuditLog> result = auditLogRepository.find(
            page, size, sort, from, to, actorId, resourceType, resourceId, namespace
        );
        return HttpResponse.ok(PagedResults.of(result));
    }
}
