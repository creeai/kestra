package io.kestra.core.models.secret;

import io.kestra.core.models.HasUID;
import io.kestra.core.models.TenantInterface;
import io.kestra.core.utils.IdUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;

/**
 * Namespace-scoped secret stored in the repository (e.g. database).
 * Value is stored encrypted. Use only for persistence; never log the value.
 */
@SuperBuilder
@ToString(exclude = "valueEncrypted")
@Getter
@Setter
@NoArgsConstructor
public class NamespaceSecret implements HasUID, TenantInterface {
    @NotNull
    private String tenantId;

    @NotNull
    private String namespace;

    @NotNull
    private String key;

    /**
     * Encrypted value (AES/GCM). Never expose in logs or API responses.
     */
    @NotNull
    private String valueEncrypted;

    private String description;

    private Map<String, String> tags;

    @JsonCreator
    public NamespaceSecret(
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("namespace") String namespace,
        @JsonProperty("key") String key,
        @JsonProperty("valueEncrypted") String valueEncrypted,
        @JsonProperty("description") String description,
        @JsonProperty("tags") Map<String, String> tags
    ) {
        this.tenantId = tenantId;
        this.namespace = namespace;
        this.key = key;
        this.valueEncrypted = valueEncrypted;
        this.description = description;
        this.tags = tags != null ? tags : Map.of();
    }

    @Override
    public String uid() {
        return IdUtils.fromParts(tenantId, namespace, key);
    }
}
