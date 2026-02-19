package io.kestra.webserver.controllers.api;

import io.kestra.core.models.auth.Action;
import io.kestra.core.models.auth.Permission;
import io.kestra.core.models.auth.Role;
import io.kestra.core.repositories.RoleRepositoryInterface;
import io.kestra.core.utils.IdUtils;
import io.kestra.webserver.filter.AuthenticationFilter;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Controller("/api/v1/roles")
@ExecuteOn(TaskExecutors.IO)
@Requires(beans = io.kestra.webserver.services.RbacService.class)
public class RoleController {

    @Inject
    private RoleRepositoryInterface roleRepository;

    @Inject
    private RbacService rbacService;

    @Get
    @Operation(tags = {"Roles"}, summary = "List roles")
    public HttpResponse<List<Role>> list(HttpRequest<?> request) {
        if (!hasPermission(request, Permission.ROLE, Action.READ)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        return HttpResponse.ok(roleRepository.findAll());
    }

    @Get("/{id}")
    @Operation(tags = {"Roles"}, summary = "Get role by id")
    public HttpResponse<Role> get(HttpRequest<?> request, @PathVariable String id) {
        if (!hasPermission(request, Permission.ROLE, Action.READ)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        Optional<Role> role = roleRepository.findById(id);
        return role.map(HttpResponse::ok).orElse(HttpResponse.status(HttpStatus.NOT_FOUND));
    }

    @Post
    @Operation(tags = {"Roles"}, summary = "Create role")
    public HttpResponse<Role> create(HttpRequest<?> request, @Body CreateRoleRequest body) {
        if (!hasPermission(request, Permission.ROLE, Action.CREATE)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        Role role = Role.builder()
            .id(IdUtils.create())
            .name(body.name)
            .description(body.description)
            .permissions(body.permissions != null ? body.permissions : Set.of())
            .build();
        roleRepository.save(role);
        return HttpResponse.ok(role);
    }

    @Patch("/{id}")
    @Operation(tags = {"Roles"}, summary = "Update role")
    public HttpResponse<Role> update(HttpRequest<?> request, @PathVariable String id, @Body UpdateRoleRequest body) {
        if (!hasPermission(request, Permission.ROLE, Action.UPDATE)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        Optional<Role> existing = roleRepository.findById(id);
        if (existing.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_FOUND);
        }
        Role r = existing.get();
        Role updated = Role.builder()
            .id(r.getId())
            .name(body.name != null ? body.name : r.getName())
            .description(body.description != null ? body.description : r.getDescription())
            .permissions(body.permissions != null ? body.permissions : r.getPermissions())
            .build();
        roleRepository.save(updated);
        return HttpResponse.ok(updated);
    }

    @Delete("/{id}")
    @Operation(tags = {"Roles"}, summary = "Delete role")
    public HttpResponse<Void> delete(HttpRequest<?> request, @PathVariable String id) {
        if (!hasPermission(request, Permission.ROLE, Action.DELETE)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        Optional<Role> role = roleRepository.findById(id);
        if (role.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_FOUND);
        }
        roleRepository.delete(role.get());
        return HttpResponse.status(HttpStatus.NO_CONTENT);
    }

    private boolean hasPermission(HttpRequest<?> request, Permission permission, Action action) {
        var user = currentUser(request);
        return user != null && rbacService.hasPermission(user, permission, action);
    }

    private io.kestra.core.models.auth.User currentUser(HttpRequest<?> request) {
        return request.getAttributes().get(AuthenticationFilter.REQUEST_ATTRIBUTE_USER, io.kestra.core.models.auth.User.class).orElse(null);
    }

    public record CreateRoleRequest(String name, String description, Set<String> permissions) {}
    public record UpdateRoleRequest(String name, String description, Set<String> permissions) {}
}
