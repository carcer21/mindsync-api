package com.carlos.mindsync.application.port.in;

import java.util.UUID;

public interface DeleteAccountUseCase {

	// Deletes the user and, with them, every journal entry they own.
	void delete(UUID userId);
}
