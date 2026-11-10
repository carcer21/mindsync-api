package com.carlos.mindsync.application.port.in;

import com.carlos.mindsync.domain.model.User;

import java.util.UUID;

public interface GetUserUseCase {

	User getById(UUID id);
}
