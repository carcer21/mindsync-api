package com.carlos.mindsync.application.port.in;

import java.util.UUID;

public interface GetAdviceUseCase {

	// Advice based on the latest entries of that user only.
	String getAdvice(UUID userId);
}
