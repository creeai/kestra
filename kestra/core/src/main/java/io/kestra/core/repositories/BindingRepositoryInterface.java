package io.kestra.core.repositories;

import io.kestra.core.models.auth.Binding;

import java.util.List;
import java.util.Optional;

public interface BindingRepositoryInterface {

    Optional<Binding> findById(String id);

    List<Binding> findByUserId(String userId);

    List<Binding> findByRoleId(String roleId);

    List<Binding> findAll();

    Binding save(Binding binding);

    Binding delete(Binding binding);
}
