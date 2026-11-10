package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.application.port.in.CreateEntryUseCase;
import com.carlos.mindsync.application.port.in.DeleteEntryUseCase;
import com.carlos.mindsync.application.port.in.GetEntryUseCase;
import com.carlos.mindsync.application.port.in.ListEntriesUseCase;
import com.carlos.mindsync.domain.model.JournalEntry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

// Every method takes the owner from the validated token, never from the request.
@Tag(name = "Journal entries", description = "Always the token owner's entries")
@RestController
@RequestMapping("/api/entries")
class JournalEntryController {

	private final CreateEntryUseCase createEntry;
	private final ListEntriesUseCase listEntries;
	private final GetEntryUseCase getEntry;
	private final DeleteEntryUseCase deleteEntry;

	JournalEntryController(CreateEntryUseCase createEntry, ListEntriesUseCase listEntries,
			GetEntryUseCase getEntry, DeleteEntryUseCase deleteEntry) {
		this.createEntry = createEntry;
		this.listEntries = listEntries;
		this.getEntry = getEntry;
		this.deleteEntry = deleteEntry;
	}

	@Operation(summary = "Create an entry")
	@PostMapping
	ResponseEntity<EntryResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EntryRequest request) {
		JournalEntry entry = createEntry.create(new CreateEntryUseCase.Command(userId(jwt), request.date(),
				request.energy(), request.emotion(), request.notes(), request.tags()));
		return ResponseEntity.created(URI.create("/api/entries/" + entry.id())).body(EntryResponse.from(entry));
	}

	@Operation(summary = "List my entries, newest first (paginated)")
	@GetMapping
	PageResponse<EntryResponse> list(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(listEntries.list(userId(jwt), page, size), EntryResponse::from);
	}

	@Operation(summary = "Get one of my entries")
	@GetMapping("/{id}")
	EntryResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		return EntryResponse.from(getEntry.get(userId(jwt), id));
	}

	@Operation(summary = "Delete one of my entries")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
		deleteEntry.delete(userId(jwt), id);
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
