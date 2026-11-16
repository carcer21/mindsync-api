package com.carlos.mindsync.infrastructure.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.function.Function;

// Runs before the controller. Over the limit, the request stops here: no password is checked, no AI call is made.
class RateLimitInterceptor implements HandlerInterceptor {

	private final FixedWindowRateLimiter limiter;
	private final Function<HttpServletRequest, String> keyOf;

	RateLimitInterceptor(FixedWindowRateLimiter limiter, Function<HttpServletRequest, String> keyOf) {
		this.limiter = limiter;
		this.keyOf = keyOf;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (limiter.tryAcquire(keyOf.apply(request))) {
			return true;
		}
		response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(limiter.secondsUntilReset()));
		// GlobalExceptionHandler turns this into application/problem+json.
		throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "too many requests, try again later");
	}
}
