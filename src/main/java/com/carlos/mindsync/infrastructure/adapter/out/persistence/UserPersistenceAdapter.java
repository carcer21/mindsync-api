package com.carlos.mindsync.infrastructure.adapter.out.persistence;

import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.domain.exception.EmailAlreadyExistsException;
import com.carlos.mindsync.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class UserPersistenceAdapter implements UserRepositoryPort {

	private final SpringDataUserRepository repository;

	UserPersistenceAdapter(SpringDataUserRepository repository) {
		this.repository = repository;
	}

	@Override
	public User save(User user) {
		try {
			return UserMapper.toDomain(repository.saveAndFlush(UserMapper.toEntity(user)));
		} catch (DataIntegrityViolationException e) {
			// The UNIQUE constraint is the real guard against concurrent registrations.
			throw new EmailAlreadyExistsException();
		}
	}

	@Override
	public Optional<User> findByEmail(String email) {
		return repository.findByEmail(email).map(UserMapper::toDomain);
	}

	@Override
	public boolean existsByEmail(String email) {
		return repository.existsByEmail(email);
	}
}
