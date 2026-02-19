package io.kestra.webserver.controllers.api;

import io.kestra.core.models.namespaces.NamespaceSettings;
import io.kestra.core.repositories.NamespaceSettingsRepositoryInterface;
import io.kestra.core.tenant.TenantService;
import io.kestra.webserver.models.api.namespace.UpdateNamespaceSettingsRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Inject;

import java.util.Optional;

@Validated
@Controller("/api/v1/{tenant}/namespaces")
public class NamespaceSettingsController {

    @Inject
    protected TenantService tenantService;

    @Inject
    protected Optional<NamespaceSettingsRepositoryInterface> namespaceSettingsRepository;

    @Get(uri = "{namespace}/settings")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "Get namespace settings")
    public HttpResponse<NamespaceSettings> getSettings(
        @Parameter(description = "The namespace id") @PathVariable String namespace
    ) {
        if (namespaceSettingsRepository.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED);
        }
        String tenantId = tenantService.resolveTenant();
        return namespaceSettingsRepository.get().findByNamespace(tenantId, namespace)
            .map(HttpResponse::ok)
            .orElseGet(() -> HttpResponse.ok(NamespaceSettings.builder()
                .tenantId(tenantId)
                .namespace(namespace)
                .description(null)
                .allowedNamespaces(null)
                .build()));
    }

    @Patch(uri = "{namespace}/settings")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "Update namespace settings")
    public HttpResponse<NamespaceSettings> updateSettings(
        @Parameter(description = "The namespace id") @PathVariable String namespace,
        @Body UpdateNamespaceSettingsRequest body
    ) {
        if (namespaceSettingsRepository.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED);
        }
        String tenantId = tenantService.resolveTenant();
        NamespaceSettings existing = namespaceSettingsRepository.get().findByNamespace(tenantId, namespace)
            .orElse(NamespaceSettings.builder().tenantId(tenantId).namespace(namespace).build());
        NamespaceSettings updated = NamespaceSettings.builder()
            .tenantId(tenantId)
            .namespace(namespace)
            .description(body.getDescription() != null ? body.getDescription() : existing.getDescription())
            .allowedNamespaces(body.getAllowedNamespaces() != null ? body.getAllowedNamespaces() : existing.getAllowedNamespaces())
            .build();
        return HttpResponse.ok(namespaceSettingsRepository.get().save(updated));
    }
}
