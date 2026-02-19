package io.kestra.core.models.auth;

import io.kestra.core.models.HasUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class Role implements HasUID {

    @NotNull
    private String id;

    @NotNull
    private String name;

    private String description;

    /**
     * Set of "permission:action" strings (e.g. "FLOW:READ", "USER:CREATE").
     */
    @NotNull
    private Set<String> permissions;

    @Override
    public String uid() {
        return id;
    }

    public boolean hasPermission(Permission permission, Action action) {
        return permissions != null && permissions.contains(permission.name() + ":" + action.name());
    }
}
