package com.carlos.mindsync.application.service;

import com.carlos.mindsync.application.port.in.GetAdviceUseCase;
import com.carlos.mindsync.application.port.out.AiAdvisorPort;
import com.carlos.mindsync.application.port.out.JournalEntryRepositoryPort;
import com.carlos.mindsync.domain.model.JournalEntry;

import java.util.List;
import java.util.UUID;

public class GetAdviceService implements GetAdviceUseCase {

	static final int ENTRIES_TO_ANALYZE = 14;
	static final String NO_ENTRIES_MESSAGE = "Todavía no tienes entradas en tu diario. "
			+ "Escribe alguna y vuelve a pedir tu análisis.";

	private final JournalEntryRepositoryPort entries;
	private final AiAdvisorPort advisor;

	public GetAdviceService(JournalEntryRepositoryPort entries, AiAdvisorPort advisor) {
		this.entries = entries;
		this.advisor = advisor;
	}

	@Override
	public String getAdvice(UUID userId) {
		// The repository returns newest first and only this user's entries.
		List<JournalEntry> latest = entries.findByUserId(userId, 0, ENTRIES_TO_ANALYZE).content();
		if (latest.isEmpty()) {
			// Nothing to analyze: do not call the AI provider or send anything outside.
			return NO_ENTRIES_MESSAGE;
		}
		return advisor.getAdvice(latest);
	}
}
