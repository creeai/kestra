package io.kestra.webserver.controllers.api;

import io.kestra.core.encryption.EncryptionService;
import io.kestra.core.models.QueryFilter;
import io.kestra.core.models.secret.NamespaceSecret;
import io.kestra.core.repositories.ArrayListTotal;
import io.kestra.core.repositories.NamespaceSecretRepositoryInterface;
import io.kestra.core.secret.SecretService;
import io.kestra.core.tenant.TenantService;
import io.kestra.webserver.converters.QueryFilterFormat;
import io.kestra.webserver.models.api.secret.ApiSecretListResponse;
import io.kestra.webserver.models.api.secret.ApiSecretMeta;
import io.kestra.webserver.models.api.secret.CreateOrUpdateNamespaceSecretRequest;
import io.kestra.webserver.utils.Searcheable;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.inject.Inject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Validated
@Controller("/api/v1/{tenant}/namespaces")
public class NamespaceSecretController<META extends ApiSecretMeta> {
    @Inject
    protected TenantService tenantService;

    @Inject
    protected SecretService<String> secretService;

    @Inject
    protected Optional<NamespaceSecretRepositoryInterface> namespaceSecretRepository;

    @Value("${kestra.encryption.secret-key:}")
    protected Optional<String> encryptionKey;

    @Get(uri = "{namespace}/secrets")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "Get secrets for a namespace")
    @Deprecated
    public HttpResponse<ApiSecretListResponse<META>> listNamespaceSecrets(
        @Parameter(description = "The namespace id") @PathVariable String namespace,
        @Parameter(description = "The current page") @QueryValue(value = "page", defaultValue = "1") int page,
        @Parameter(description = "The current page size") @QueryValue(value = "size", defaultValue = "10") int size,
        @Parameter(description = "The sort of current page") @Nullable @QueryValue(value = "sort") List<String> sort,
        @Parameter(description = "Filters", in = ParameterIn.QUERY) @QueryFilterFormat List<QueryFilter> filters
    ) throws IllegalArgumentException, IOException {
        final String tenantId = this.tenantService.resolveTenant();
        Set<String> inheritedKeys = secretService.inheritedSecrets(tenantId, namespace).get(namespace);

        List<ApiSecretMeta> allItems = new ArrayList<>();
        if (namespaceSecretRepository.isPresent()) {
            List<NamespaceSecret> fromDb = namespaceSecretRepository.get().list(tenantId, namespace);
            for (NamespaceSecret s : fromDb) {
                allItems.add(new ApiSecretMeta(s.getKey(), s.getDescription(), s.getTags()));
            }
        }
        for (String key : inheritedKeys) {
            if (allItems.stream().noneMatch(m -> m.getKey().equals(key))) {
                allItems.add(new ApiSecretMeta(key));
            }
        }

        final String query = filters.stream()
            .filter(filter -> filter.field().equals(QueryFilter.Field.QUERY))
            .map(QueryFilter::value)
            .map(Object::toString)
            .findFirst()
            .orElse(null);

        final ArrayListTotal<ApiSecretMeta> results = Searcheable.of(allItems)
            .search(Searcheable.Searched.<ApiSecretMeta>builder()
                .query(query)
                .size(size)
                .sort(sort)
                .page(page)
                .sortableExtractor("key", ApiSecretMeta::getKey)
                .searchableExtractor("key", ApiSecretMeta::getKey)
                .build()
            );

        //noinspection unchecked
        return HttpResponse.ok((ApiSecretListResponse<META>) new ApiSecretListResponse<>(
                true,
                results,
                results.getTotal()
            )
        );
    }

    @Get(uri = "{namespace}/inherited-secrets")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "List inherited secrets")
    public HttpResponse<Map<String, Set<String>>> getInheritedSecrets(
        @Parameter(description = "The namespace id") @PathVariable String namespace
    ) throws IllegalArgumentException, IOException {
        return HttpResponse.ok(secretService.inheritedSecrets(tenantService.resolveTenant(), namespace));
    }

    @Post(uri = "{namespace}/secrets")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "Create or replace a namespace secret")
    public HttpResponse<?> createNamespaceSecret(
        @Parameter(description = "The namespace id") @PathVariable String namespace,
        @Body CreateOrUpdateNamespaceSecretRequest body
    ) {
        if (namespaceSecretRepository.isEmpty() || encryptionKey.isEmpty() || encryptionKey.get().isBlank()) {
            return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED);
        }
        String tenantId = tenantService.resolveTenant();
        String keyUpper = body.getKey().toUpperCase();
        if (body.getValue() == null || body.getValue().isBlank()) {
            return HttpResponse.badRequest("Secret value is required");
        }
        try {
            String encrypted = EncryptionService.encrypt(encryptionKey.get(), body.getValue());
            NamespaceSecret secret = NamespaceSecret.builder()
                .tenantId(tenantId)
                .namespace(namespace)
                .key(keyUpper)
                .valueEncrypted(encrypted)
                .description(body.getDescription())
                .tags(body.getTags() != null ? body.getTags() : Map.of())
                .build();
            namespaceSecretRepository.get().save(secret);
            return HttpResponse.noContent();
        } catch (GeneralSecurityException e) {
            return HttpResponse.serverError("Failed to encrypt secret");
        }
    }

    @Patch(uri = "{namespace}/secrets/{key}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "Update a namespace secret")
    public HttpResponse<?> updateNamespaceSecret(
        @Parameter(description = "The namespace id") @PathVariable String namespace,
        @Parameter(description = "The secret key") @PathVariable String key,
        @Body CreateOrUpdateNamespaceSecretRequest body
    ) {
        if (namespaceSecretRepository.isEmpty() || encryptionKey.isEmpty() || encryptionKey.get().isBlank()) {
            return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED);
        }
        String tenantId = tenantService.resolveTenant();
        String keyUpper = key.toUpperCase();
        var existing = namespaceSecretRepository.get().findByNamespaceAndKey(tenantId, namespace, keyUpper);
        if (existing.isEmpty()) {
            return HttpResponse.notFound();
        }
        try {
            String valueEncrypted = existing.get().getValueEncrypted();
            if (body.getValue() != null && !body.getValue().isBlank()) {
                valueEncrypted = EncryptionService.encrypt(encryptionKey.get(), body.getValue());
            }
            Map<String, String> tags = body.getTags() != null ? body.getTags() : existing.get().getTags();
            NamespaceSecret secret = NamespaceSecret.builder()
                .tenantId(tenantId)
                .namespace(namespace)
                .key(keyUpper)
                .valueEncrypted(valueEncrypted)
                .description(body.getDescription() != null ? body.getDescription() : existing.get().getDescription())
                .tags(tags != null ? tags : Map.of())
                .build();
            namespaceSecretRepository.get().save(secret);
            return HttpResponse.noContent();
        } catch (GeneralSecurityException e) {
            return HttpResponse.serverError("Failed to encrypt secret");
        }
    }

    @Delete(uri = "{namespace}/secrets/{key}")
    @ExecuteOn(TaskExecutors.IO)
    @Operation(tags = {"Namespaces"}, summary = "Delete a namespace secret")
    public HttpResponse<?> deleteNamespaceSecret(
        @Parameter(description = "The namespace id") @PathVariable String namespace,
        @Parameter(description = "The secret key") @PathVariable String key
    ) {
        if (namespaceSecretRepository.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_IMPLEMENTED);
        }
        String tenantId = tenantService.resolveTenant();
        namespaceSecretRepository.get().delete(tenantId, namespace, key.toUpperCase());
        return HttpResponse.noContent();
    }
}
