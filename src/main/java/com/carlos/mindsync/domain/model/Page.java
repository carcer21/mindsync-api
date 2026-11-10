package com.carlos.mindsync.domain.model;

import java.util.List;

public record Page<T>(List<T> content, int page, int size, long totalElements) {

	public Page {
		content = List.copyOf(content);
	}

	public int totalPages() {
		return (int) ((totalElements + size - 1) / size);
	}
}
