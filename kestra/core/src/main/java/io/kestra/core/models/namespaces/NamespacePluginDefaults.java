package io.kestra.core.models.namespaces;

import io.kestra.core.models.HasUID;
import io.kestra.core.models.TenantInterface;
import io.kestra.core.models.flows.PluginDefault;
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
 * Namespace-level plugin defaults. Applied to all tasks of the corresponding type in flows under this namespace.
 */
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class NamespacePluginDefaults implements HasUID, TenantInterface {
    @NotNull
    private String tenantId;

    @NotNull
    private String namespace;

    @NotNull
    private List<PluginDefault> defaults;

    @JsonCreator
    public NamespacePluginDefaults(
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("namespace") String namespace,
        @JsonProperty("defaults") List<PluginDefault> defaults
    ) {
        this.tenantId = tenantId;
        this.namespace = namespace;
        this.defaults = defaults != null ? defaults : List.of();
    }

    @Override
    public String uid() {
        return IdUtils.fromParts(tenantId, namespace);
    }
}
