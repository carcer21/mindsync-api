package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.GetUserUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
class UserController {

	private final GetUserUseCase getUser;

	UserController(GetUserUseCase getUser) {
		this.getUser = getUser;
	}

	// The user id comes from the validated token, never from a client-supplied parameter.
	@GetMapping("/me")
	UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return UserResponse.from(getUser.getById(UUID.fromString(jwt.getSubject())));
	}
}
