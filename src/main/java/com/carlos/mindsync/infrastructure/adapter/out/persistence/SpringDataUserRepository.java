package com.carlos.mindsync.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

	Optional<UserEntity> findByEmail(String email);

	boolean existsByEmail(String email);

	// "remove" and not "delete": JpaRepository already has a deleteById that returns nothing.
	// The journal entries and their tags go with the user through ON DELETE CASCADE in the database.
	@Transactional
	long removeById(UUID id);
}
