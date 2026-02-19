package io.kestra.core.models.audit;

import io.kestra.core.models.HasUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public class AuditLog implements HasUID {

    @NotNull
    private String id;

    @NotNull
    private Instant timestamp;

    @NotNull
    private String actorId;

    private AuditActorType actorType;

    @NotNull
    private AuditAction action;

    @NotNull
    private String resourceType;

    private String resourceId;

    private String namespace;

    private String tenantId;

    /**
     * Optional JSON-serializable details (e.g. diff, extra context).
     */
    private Map<String, Object> details;

    @Override
    public String uid() {
        return id;
    }

    public enum AuditActorType {
        USER,
        SERVICE_ACCOUNT
    }

    public enum AuditAction {
        CREATE,
        READ,
        UPDATE,
        DELETE
    }
}
