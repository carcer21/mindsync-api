package com.carlos.mindsync.infrastructure.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RegisterRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 8, max = 72) String password) {

	// Never print the raw password.
	@Override
	public String toString() {
		return "RegisterRequest[name=" + name + ", email=" + email + "]";
	}
}
