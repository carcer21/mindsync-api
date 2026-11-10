package com.carlos.mindsync.application.port.out;

import com.carlos.mindsync.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

	User save(User user);

	Optional<User> findById(UUID id);

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}
