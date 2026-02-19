package io.kestra.core.repositories;

import io.kestra.core.models.auth.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepositoryInterface {

    Optional<Role> findById(String id);

    List<Role> findAll();

    Role save(Role role);

    Role delete(Role role);
}
