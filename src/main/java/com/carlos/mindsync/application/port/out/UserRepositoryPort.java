package com.carlos.mindsync.application.port.out;

import com.carlos.mindsync.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

	User save(User user);

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}
