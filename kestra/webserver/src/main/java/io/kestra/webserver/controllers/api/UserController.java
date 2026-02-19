package io.kestra.webserver.controllers.api;

import io.kestra.core.models.audit.AuditLog;
import io.kestra.core.models.auth.Action;
import io.kestra.core.models.auth.Permission;
import io.kestra.core.models.auth.User;
import io.kestra.core.repositories.UserRepositoryInterface;
import io.kestra.core.utils.AuthUtils;
import io.kestra.core.utils.IdUtils;
import io.kestra.webserver.filter.AuthenticationFilter;
import io.kestra.webserver.services.AuditService;
import io.kestra.webserver.services.RbacService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.context.annotation.Requires;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller("/api/v1/users")
@ExecuteOn(TaskExecutors.IO)
@Requires(beans = io.kestra.webserver.services.RbacService.class)
public class UserController {

    @Inject
    private UserRepositoryInterface userRepository;

    @Inject
    private RbacService rbacService;

    @Inject
    private Optional<AuditService> auditService;

    private static User sanitize(User user) {
        return User.builder()
            .id(user.getId())
            .username(user.getUsername())
            .passwordHash("")
            .salt(null)
            .disabled(user.isDisabled())
            .createdAt(user.getCreatedAt())
            .build();
    }

    @Get
    @Operation(tags = {"Users"}, summary = "List users")
    public HttpResponse<List<User>> list(HttpRequest<?> request) {
        User current = currentUser(request);
        if (current == null || !rbacService.hasPermission(current, Permission.USER, Action.READ)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        return HttpResponse.ok(userRepository.findAll().stream().map(UserController::sanitize).toList());
    }

    @Get("/{id}")
    @Operation(tags = {"Users"}, summary = "Get user by id")
    public HttpResponse<User> get(HttpRequest<?> request, @PathVariable String id) {
        User current = currentUser(request);
        if (current == null || !rbacService.hasPermission(current, Permission.USER, Action.READ)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        Optional<User> user = userRepository.findById(id);
        return user.map(u -> HttpResponse.ok(sanitize(u))).orElse(HttpResponse.status(HttpStatus.NOT_FOUND));
    }

    @Post
    @Operation(tags = {"Users"}, summary = "Create user")
    public HttpResponse<User> create(HttpRequest<?> request, @Body CreateUserRequest body) {
        User current = currentUser(request);
        if (current == null || !rbacService.hasPermission(current, Permission.USER, Action.CREATE)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        if (userRepository.findByUsername(body.username).isPresent()) {
            return HttpResponse.status(HttpStatus.CONFLICT);
        }
        String salt = AuthUtils.generateSalt();
        String passwordHash = AuthUtils.encodePassword(salt, body.password);
        User user = User.builder()
            .id(IdUtils.create())
            .username(body.username)
            .passwordHash(passwordHash)
            .salt(salt)
            .disabled(body.disabled != null && body.disabled)
            .createdAt(Instant.now())
            .build();
        userRepository.save(user);
        auditService.ifPresent(a -> a.log(currentUser(request), AuditLog.AuditAction.CREATE, "USER", user.getId(), null, Map.of("username", user.getUsername())));
        return HttpResponse.ok(sanitize(user));
    }

    @Patch("/{id}")
    @Operation(tags = {"Users"}, summary = "Update user")
    public HttpResponse<User> update(HttpRequest<?> request, @PathVariable String id, @Body UpdateUserRequest body) {
        User current = currentUser(request);
        if (current == null || !rbacService.hasPermission(current, Permission.USER, Action.UPDATE)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_FOUND);
        }
        User u = existing.get();
        String salt = u.getSalt() != null ? u.getSalt() : AuthUtils.generateSalt();
        String passwordHash = body.password != null ? AuthUtils.encodePassword(salt, body.password) : u.getPasswordHash();
        if (body.password != null && u.getSalt() == null) {
            salt = AuthUtils.generateSalt();
            passwordHash = AuthUtils.encodePassword(salt, body.password);
        }
        User updated = User.builder()
            .id(u.getId())
            .username(body.username != null ? body.username : u.getUsername())
            .passwordHash(passwordHash)
            .salt(salt)
            .disabled(body.disabled != null ? body.disabled : u.isDisabled())
            .createdAt(u.getCreatedAt())
            .build();
        userRepository.save(updated);
        auditService.ifPresent(a -> a.log(currentUser(request), AuditLog.AuditAction.UPDATE, "USER", updated.getId(), null, Map.of("username", updated.getUsername())));
        return HttpResponse.ok(sanitize(updated));
    }

    @Delete("/{id}")
    @Operation(tags = {"Users"}, summary = "Delete user")
    public HttpResponse<Void> delete(HttpRequest<?> request, @PathVariable String id) {
        User current = currentUser(request);
        if (current == null || !rbacService.hasPermission(current, Permission.USER, Action.DELETE)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_FOUND);
        }
        User deleted = user.get();
        userRepository.delete(deleted);
        auditService.ifPresent(a -> a.log(currentUser(request), AuditLog.AuditAction.DELETE, "USER", deleted.getId(), null, Map.of("username", deleted.getUsername())));
        return HttpResponse.status(HttpStatus.NO_CONTENT);
    }

    @Post("/{id}/reset-password")
    @Operation(tags = {"Users"}, summary = "Reset user password")
    public HttpResponse<User> resetPassword(HttpRequest<?> request, @PathVariable String id, @Body ResetPasswordRequest body) {
        User current = currentUser(request);
        if (current == null || (!current.getId().equals(id) && !rbacService.hasPermission(current, Permission.USER, Action.UPDATE))) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_FOUND);
        }
        User u = existing.get();
        String salt = u.getSalt() != null ? u.getSalt() : AuthUtils.generateSalt();
        String passwordHash = AuthUtils.encodePassword(salt, body.newPassword);
        User updated = User.builder()
            .id(u.getId())
            .username(u.getUsername())
            .passwordHash(passwordHash)
            .salt(salt)
            .disabled(u.isDisabled())
            .createdAt(u.getCreatedAt())
            .build();
        userRepository.save(updated);
        auditService.ifPresent(a -> a.log(currentUser(request), AuditLog.AuditAction.UPDATE, "USER", updated.getId(), null, Map.of("username", updated.getUsername(), "action", "reset-password")));
        return HttpResponse.ok(sanitize(updated));
    }

    private User currentUser(HttpRequest<?> request) {
        return request.getAttributes().get(AuthenticationFilter.REQUEST_ATTRIBUTE_USER, User.class).orElse(null);
    }

    public record CreateUserRequest(String username, String password, Boolean disabled) {}
    public record UpdateUserRequest(String username, String password, Boolean disabled) {}
    public record ResetPasswordRequest(String newPassword) {}
}
