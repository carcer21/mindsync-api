package com.carlos.mindsync.application.port.in;

import com.carlos.mindsync.domain.model.User;

public interface RegisterUserUseCase {

	User register(Command command);

	record Command(String name, String email, String password) {

		// Never print the raw password.
		@Override
		public String toString() {
			return "Command[name=" + name + ", email=" + email + "]";
		}
	}
}
