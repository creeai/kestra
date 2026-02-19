package io.kestra.core.models.auth;

import io.kestra.core.models.HasUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@SuperBuilder
@ToString(exclude = "passwordHash")
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class User implements HasUID {

    @NotNull
    private String id;

    @NotNull
    private String username;

    @NotNull
    private String passwordHash;

    private String salt;

    private boolean disabled;

    @NotNull
    private Instant createdAt;

    @Override
    public String uid() {
        return id;
    }
}
