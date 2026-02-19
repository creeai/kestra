package io.kestra.webserver.controllers.api;

import io.kestra.core.models.flows.PluginDefault;
import io.kestra.core.models.namespaces.NamespacePluginDefaults;
import io.kestra.core.repositories.NamespacePluginDefaultsRepositoryInterface;
import io.kestra.core.tenant.TenantService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Put;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@Validated
@Controller("/api/v1/{tenant}/namespaces")
public class NamespacePluginDefaultsController {

    @Inject
    protected TenantService tenantService;

    @Inject
    protected Optional<NamespacePluginDefaultsRepositoryInterface> namespacePluginDefaultsRepository;

    @Get(uri = "{namespace}/plugin-defaults")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "Get namespace plugin defaults")
    public HttpResponse<List<PluginDefault>> getPluginDefaults(
        @Parameter(description = "The namespace id") @PathVariable String namespace
    ) {
        if (namespacePluginDefaultsRepository.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED);
        }
        String tenantId = tenantService.resolveTenant();
        List<PluginDefault> defaults = namespacePluginDefaultsRepository.get().getDefaultsByNamespace(tenantId, namespace);
        return HttpResponse.ok(defaults);
    }

    @Put(uri = "{namespace}/plugin-defaults")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "Set namespace plugin defaults")
    public HttpResponse<?> setPluginDefaults(
        @Parameter(description = "The namespace id") @PathVariable String namespace,
        @Body List<PluginDefault> defaults
    ) {
        if (namespacePluginDefaultsRepository.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED);
        }
        String tenantId = tenantService.resolveTenant();
        NamespacePluginDefaults entity = NamespacePluginDefaults.builder()
            .tenantId(tenantId)
            .namespace(namespace)
            .defaults(defaults != null ? defaults : List.of())
            .build();
        namespacePluginDefaultsRepository.get().save(entity);
        return HttpResponse.noContent();
    }
}
