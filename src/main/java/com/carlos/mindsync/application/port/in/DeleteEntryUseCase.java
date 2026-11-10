package com.carlos.mindsync.application.port.in;

import java.util.UUID;

public interface DeleteEntryUseCase {

	void delete(UUID userId, UUID entryId);
}
