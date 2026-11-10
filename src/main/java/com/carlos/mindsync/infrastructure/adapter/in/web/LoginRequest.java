package com.carlos.mindsync.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(@NotBlank String email, @NotBlank String password) {

	// Never print the raw password.
	@Override
	public String toString() {
		return "LoginRequest[email=" + email + "]";
	}
}
