package io.kestra.webserver.models.api.secret;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.micronaut.core.annotation.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateOrUpdateNamespaceSecretRequest", description = "Request body for creating or updating a namespace secret")
public class CreateOrUpdateNamespaceSecretRequest {
    @NotNull
    @NotBlank
    @Schema(description = "Secret key", required = true)
    private String key;

    @Nullable
    @Schema(description = "Secret value (plain text; will be encrypted at rest)")
    private String value;

    @Nullable
    @Schema(description = "Optional description")
    private String description;

    @Nullable
    @Schema(description = "Optional tags (key-value map, or array of {key, value})")
    @JsonDeserialize(using = TagsDeserializer.class)
    private Map<String, String> tags;

    /**
     * Accepts both JSON object (map) and array of {key, value} for UI compatibility.
     */
    public static class TagsDeserializer extends JsonDeserializer<Map<String, String>> {
        @Override
        public Map<String, String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == null) {
                p.nextToken();
            }
            if (p.isExpectedStartArrayToken()) {
                List<?> list = ctxt.readValue(p, List.class);
                Map<String, String> map = new LinkedHashMap<>();
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> entry = (Map<String, Object>) item;
                        Object k = entry.get("key");
                        Object v = entry.get("value");
                        if (k != null && !String.valueOf(k).isBlank()) {
                            map.put(String.valueOf(k), v != null ? String.valueOf(v) : "");
                        }
                    }
                }
                return map;
            }
            if (p.isExpectedStartObjectToken()) {
                return ctxt.readValue(p, ctxt.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class));
            }
            return null;
        }
    }
}
