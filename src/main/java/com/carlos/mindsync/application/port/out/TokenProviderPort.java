package com.carlos.mindsync.application.port.out;

import com.carlos.mindsync.domain.model.User;

public interface TokenProviderPort {

	String generateToken(User user);
}
