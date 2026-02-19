package io.kestra.webserver.controllers.api;

import io.kestra.core.models.auth.Action;
import io.kestra.core.models.auth.Binding;
import io.kestra.core.models.auth.Permission;
import io.kestra.core.repositories.BindingRepositoryInterface;
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
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.context.annotation.Requires;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@Controller("/api/v1/bindings")
@ExecuteOn(TaskExecutors.IO)
@Requires(beans = io.kestra.webserver.services.RbacService.class)
public class BindingController {

    @Inject
    private BindingRepositoryInterface bindingRepository;

    @Inject
    private RbacService rbacService;

    @Get
    @Operation(tags = {"Bindings"}, summary = "List bindings")
    public HttpResponse<List<Binding>> list(
        HttpRequest<?> request,
        @Nullable @QueryValue String userId,
        @Nullable @QueryValue String roleId,
        @Nullable @QueryValue String namespace
    ) {
        if (!hasPermission(request, Permission.BINDING, Action.READ)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        List<Binding> list;
        if (userId != null) {
            list = bindingRepository.findByUserId(userId);
        } else if (roleId != null) {
            list = bindingRepository.findByRoleId(roleId);
        } else {
            list = bindingRepository.findAll();
        }
        if (namespace != null) {
            list = list.stream().filter(b -> namespace.equals(b.getNamespace()) || (b.getNamespace() != null && b.getNamespace().startsWith(namespace))).toList();
        }
        return HttpResponse.ok(list);
    }

    @Get("/{id}")
    @Operation(tags = {"Bindings"}, summary = "Get binding by id")
    public HttpResponse<Binding> get(HttpRequest<?> request, @PathVariable String id) {
        if (!hasPermission(request, Permission.BINDING, Action.READ)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        Optional<Binding> binding = bindingRepository.findById(id);
        return binding.map(HttpResponse::ok).orElse(HttpResponse.status(HttpStatus.NOT_FOUND));
    }

    @Post
    @Operation(tags = {"Bindings"}, summary = "Create binding")
    public HttpResponse<Binding> create(HttpRequest<?> request, @Body CreateBindingRequest body) {
        if (!hasPermission(request, Permission.BINDING, Action.CREATE)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        if (body.userId == null && body.groupId == null) {
            return HttpResponse.status(HttpStatus.BAD_REQUEST);
        }
        Binding binding = Binding.builder()
            .id(IdUtils.create())
            .roleId(body.roleId)
            .userId(body.userId)
            .groupId(body.groupId)
            .namespace(body.namespace)
            .build();
        bindingRepository.save(binding);
        return HttpResponse.ok(binding);
    }

    @Delete("/{id}")
    @Operation(tags = {"Bindings"}, summary = "Delete binding")
    public HttpResponse<Void> delete(HttpRequest<?> request, @PathVariable String id) {
        if (!hasPermission(request, Permission.BINDING, Action.DELETE)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN);
        }
        Optional<Binding> binding = bindingRepository.findById(id);
        if (binding.isEmpty()) {
            return HttpResponse.status(HttpStatus.NOT_FOUND);
        }
        bindingRepository.delete(binding.get());
        return HttpResponse.status(HttpStatus.NO_CONTENT);
    }

    private boolean hasPermission(HttpRequest<?> request, Permission permission, Action action) {
        var user = currentUser(request);
        return user != null && rbacService.hasPermission(user, permission, action);
    }

    private io.kestra.core.models.auth.User currentUser(HttpRequest<?> request) {
        return request.getAttributes().get(AuthenticationFilter.REQUEST_ATTRIBUTE_USER, io.kestra.core.models.auth.User.class).orElse(null);
    }

    public record CreateBindingRequest(String roleId, String userId, String groupId, String namespace) {}
}
