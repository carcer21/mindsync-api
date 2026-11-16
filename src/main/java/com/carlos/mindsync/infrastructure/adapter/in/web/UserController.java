package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.DeleteAccountUseCase;
import com.carlos.mindsync.application.port.in.GetUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
class UserController {

	private final GetUserUseCase getUser;
	private final DeleteAccountUseCase deleteAccount;

	UserController(GetUserUseCase getUser, DeleteAccountUseCase deleteAccount) {
		this.getUser = getUser;
		this.deleteAccount = deleteAccount;
	}

	// The user id comes from the validated token, never from a client-supplied parameter.
	@Operation(summary = "Get my profile")
	@GetMapping("/me")
	UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return UserResponse.from(getUser.getById(UUID.fromString(jwt.getSubject())));
	}

	// There is no /api/users/{id}: the only account a user can delete is the one in their token.
	@Operation(summary = "Delete my account and all my journal entries")
	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteMe(@AuthenticationPrincipal Jwt jwt) {
		deleteAccount.delete(UUID.fromString(jwt.getSubject()));
	}
}
