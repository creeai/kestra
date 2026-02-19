package io.kestra.webserver.controllers.api;

import io.kestra.core.models.namespaces.NamespaceVariable;
import io.kestra.core.repositories.NamespaceVariableRepositoryInterface;
import io.kestra.core.tenant.TenantService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Validated
@Controller("/api/v1/{tenant}/namespaces")
public class NamespaceVariableController {

    @Inject
    protected TenantService tenantService;

    @Inject
    protected Optional<NamespaceVariableRepositoryInterface> namespaceVariableRepository;

    @Get(uri = "{namespace}/variables")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "List namespace variables")
    public HttpResponse<List<Map<String, String>>> listVariables(
        @Parameter(description = "The namespace id") @PathVariable String namespace
    ) {
        if (namespaceVariableRepository.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED);
        }
        String tenantId = tenantService.resolveTenant();
        List<NamespaceVariable> list = namespaceVariableRepository.get().list(tenantId, namespace);
        List<Map<String, String>> result = list.stream()
            .map(v -> Map.<String, String>of("key", v.getKey(), "value", v.getValue()))
            .collect(Collectors.toList());
        return HttpResponse.ok(result);
    }

    @Put(uri = "{namespace}/variables")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "Set namespace variables (replace all)")
    public HttpResponse<?> setVariables(
        @Parameter(description = "The namespace id") @PathVariable String namespace,
        @Body Map<String, String> variables
    ) {
        if (namespaceVariableRepository.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED);
        }
        String tenantId = tenantService.resolveTenant();
        for (NamespaceVariable existing : namespaceVariableRepository.get().list(tenantId, namespace)) {
            namespaceVariableRepository.get().delete(tenantId, namespace, existing.getKey());
        }
        for (Map.Entry<String, String> e : variables.entrySet()) {
            if (e.getKey() != null && !e.getKey().isBlank()) {
                namespaceVariableRepository.get().save(NamespaceVariable.builder()
                    .tenantId(tenantId)
                    .namespace(namespace)
                    .key(e.getKey())
                    .value(e.getValue() != null ? e.getValue() : "")
                    .build());
            }
        }
        return HttpResponse.noContent();
    }

    @Delete(uri = "{namespace}/variables/{key}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "Delete a namespace variable")
    public HttpResponse<?> deleteVariable(
        @Parameter(description = "The namespace id") @PathVariable String namespace,
        @Parameter(description = "The variable key") @PathVariable String key
    ) {
        if (namespaceVariableRepository.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED);
        }
        String tenantId = tenantService.resolveTenant();
        namespaceVariableRepository.get().delete(tenantId, namespace, key);
        return HttpResponse.noContent();
    }
}
