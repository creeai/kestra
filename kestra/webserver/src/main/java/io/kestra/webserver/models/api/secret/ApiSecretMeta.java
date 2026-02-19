package io.kestra.webserver.models.api.secret;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

@Getter
@EqualsAndHashCode
public class ApiSecretMeta {
    private final String key;
    private final String description;
    private final Map<String, String> tags;

    public ApiSecretMeta(
        @NotNull
        @Parameter(
            name = "key",
            description = "The key of secret.",
            required = true)
        String key
    ) {
        this.key = key;
        this.description = null;
        this.tags = null;
    }

    public ApiSecretMeta(String key, String description, Map<String, String> tags) {
        this.key = key;
        this.description = description;
        this.tags = tags != null ? tags : Map.of();
    }
}