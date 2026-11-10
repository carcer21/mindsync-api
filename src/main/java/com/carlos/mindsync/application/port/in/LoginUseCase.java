package com.carlos.mindsync.application.port.in;

public interface LoginUseCase {

	/** Returns a signed access token. */
	String login(Command command);

	record Command(String email, String password) {

		// Never print the raw password.
		@Override
		public String toString() {
			return "Command[email=" + email + "]";
		}
	}
}
