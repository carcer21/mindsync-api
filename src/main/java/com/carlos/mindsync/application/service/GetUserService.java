package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.GetUserUseCase;
import com.carlos.mindsync.application.port.out.UserRepositoryPort;
import com.carlos.mindsync.domain.exception.UserNotFoundException;
import com.carlos.mindsync.domain.model.User;

import java.util.UUID;

public class GetUserService implements GetUserUseCase {

	private final UserRepositoryPort users;

	public GetUserService(UserRepositoryPort users) {
		this.users = users;
	}

	@Override
	public User getById(UUID id) {
		return users.findById(id).orElseThrow(UserNotFoundException::new);
	}
}
