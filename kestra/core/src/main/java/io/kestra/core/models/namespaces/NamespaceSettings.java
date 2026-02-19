package io.kestra.core.models.namespaces;

import io.kestra.core.models.HasUID;
import io.kestra.core.models.TenantInterface;
import io.kestra.core.utils.IdUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Namespace-level settings: description and allowed namespaces (for access control).
 * When allowedNamespaces is null or empty, all namespaces are allowed (default).
 */
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class NamespaceSettings implements HasUID, TenantInterface {
    @NotNull
    private String tenantId;

    @NotNull
    private String namespace;

    private String description;

    /**
     * List of namespace ids that are allowed to access this namespace's resources.
     * Null or empty means all namespaces are allowed.
     */
    private List<String> allowedNamespaces;

    @JsonCreator
    public NamespaceSettings(
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("namespace") String namespace,
        @JsonProperty("description") String description,
        @JsonProperty("allowedNamespaces") List<String> allowedNamespaces
    ) {
        this.tenantId = tenantId;
        this.namespace = namespace;
        this.description = description;
        this.allowedNamespaces = allowedNamespaces;
    }

    @Override
    public String uid() {
        return IdUtils.fromParts(tenantId, namespace);
    }
}
