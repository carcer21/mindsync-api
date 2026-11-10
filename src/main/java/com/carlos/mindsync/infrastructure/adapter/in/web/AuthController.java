package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.LoginUseCase;
import com.carlos.mindsync.application.port.in.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Public: register and log in")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
class AuthController {

	private final RegisterUserUseCase registerUser;
	private final LoginUseCase login;

	AuthController(RegisterUserUseCase registerUser, LoginUseCase login) {
		this.registerUser = registerUser;
		this.login = login;
	}

	@Operation(summary = "Create an account")
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return UserResponse.from(registerUser.register(
				new RegisterUserUseCase.Command(request.name(), request.email(), request.password())));
	}

	@Operation(summary = "Log in and get a JWT access token")
	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return TokenResponse.bearer(login.login(new LoginUseCase.Command(request.email(), request.password())));
	}
}
