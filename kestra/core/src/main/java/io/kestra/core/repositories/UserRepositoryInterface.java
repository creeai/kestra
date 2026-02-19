package io.kestra.core.repositories;

import io.kestra.core.models.auth.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryInterface {

    Optional<User> findById(String id);

    Optional<User> findByUsername(String username);

    List<User> findAll();

    User save(User user);

    User delete(User user);
}
