package com.carlos.mindsync.infrastructure.adapter.out.persistence;

import com.carlos.mindsync.domain.model.User;

final class UserMapper {

	private UserMapper() {
	}

	static UserEntity toEntity(User user) {
		return new UserEntity(user.id(), user.name(), user.email(), user.passwordHash());
	}

	static User toDomain(UserEntity entity) {
		return new User(entity.getId(), entity.getName(), entity.getEmail(), entity.getPasswordHash());
	}
}
