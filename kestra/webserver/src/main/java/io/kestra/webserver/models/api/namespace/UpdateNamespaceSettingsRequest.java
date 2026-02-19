package io.kestra.webserver.models.api.namespace;

import io.micronaut.core.annotation.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "UpdateNamespaceSettingsRequest", description = "Request body for updating namespace settings")
public class UpdateNamespaceSettingsRequest {
    @Nullable
    @Schema(description = "Namespace description")
    private String description;

    @Nullable
    @Schema(description = "List of namespace ids allowed to access this namespace; null or empty means all allowed")
    private List<String> allowedNamespaces;
}
