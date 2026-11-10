package com.carlos.mindsync.application.port.out;

import com.carlos.mindsync.domain.model.JournalEntry;

import java.util.List;

// The application only knows this contract; which AI provider answers is an infrastructure detail.
public interface AiAdvisorPort {

	// Never throws for provider failures: implementations return a safe fallback text instead.
	String getAdvice(List<JournalEntry> entries);
}
