package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.domain.model.User;

import java.util.UUID;

// No password field here, so a response can never contain it.
record UserResponse(UUID id, String name, String email) {

	static UserResponse from(User user) {
		return new UserResponse(user.id(), user.name(), user.email());
	}
}
