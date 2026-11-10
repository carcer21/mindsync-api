package com.carlos.mindsync.infrastructure.adapter.in.web;

record TokenResponse(String accessToken, String tokenType) {

	static TokenResponse bearer(String accessToken) {
		return new TokenResponse(accessToken, "Bearer");
	}
}
