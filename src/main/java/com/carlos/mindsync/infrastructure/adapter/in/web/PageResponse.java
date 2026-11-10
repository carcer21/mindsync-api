package com.carlos.mindsync.infrastructure.adapter.in.web;

import com.carlos.mindsync.domain.model.Page;

import java.util.List;
import java.util.function.Function;

record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	static <D, T> PageResponse<T> from(Page<D> page, Function<D, T> mapper) {
		return new PageResponse<>(page.content().stream().map(mapper).toList(), page.page(), page.size(),
				page.totalElements(), page.totalPages());
	}
}
