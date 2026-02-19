package io.kestra.core.models.auth;

/**
 * Resource types for RBAC permission checks.
 */
public enum Permission {
    FLOW,
    EXECUTION,
    NAMESPACE,
    SECRET,
    USER,
    ROLE,
    BINDING,
    AUDIT
}
