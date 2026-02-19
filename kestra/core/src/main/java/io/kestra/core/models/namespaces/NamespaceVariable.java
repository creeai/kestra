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

/**
 * Namespace-level variable (key-value). Exposed in expressions as {{ namespace.key }}.
 */
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class NamespaceVariable implements HasUID, TenantInterface {
    @NotNull
    private String tenantId;

    @NotNull
    private String namespace;

    @NotNull
    private String key;

    @NotNull
    private String value;

    @JsonCreator
    public NamespaceVariable(
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("namespace") String namespace,
        @JsonProperty("key") String key,
        @JsonProperty("value") String value
    ) {
        this.tenantId = tenantId;
        this.namespace = namespace;
        this.key = key;
        this.value = value;
    }

    @Override
    public String uid() {
        return IdUtils.fromParts(tenantId, namespace, key);
    }
}
