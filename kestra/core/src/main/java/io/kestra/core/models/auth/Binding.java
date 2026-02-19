package io.kestra.core.models.auth;

import io.kestra.core.models.HasUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Binding implements HasUID {

    @NotNull
    private String id;

    @NotNull
    private String roleId;

    @Nullable
    private String userId;

    @Nullable
    private String groupId;

    /**
     * Optional namespace scope; null means all namespaces.
     * When set, grants access to this namespace and its children (e.g. "prod" grants "prod.team").
     */
    @Nullable
    private String namespace;

    @Override
    public String uid() {
        return id;
    }
}
