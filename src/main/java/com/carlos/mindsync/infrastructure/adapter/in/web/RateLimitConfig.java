package com.carlos.mindsync.infrastructure.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.time.InstantSource;

@Configuration
@EnableConfigurationProperties(RateLimitConfig.Properties.class)
class RateLimitConfig implements WebMvcConfigurer {

	@ConfigurationProperties("app.rate-limit")
	record Properties(Rule auth, Rule ai) {

		record Rule(int limit, Duration window) {

			FixedWindowRateLimiter newLimiter() {
				return new FixedWindowRateLimiter(limit, window, InstantSource.system());
			}
		}
	}

	private final Properties properties;

	RateLimitConfig(Properties properties) {
		this.properties = properties;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// Nobody is logged in yet, so the key is the client address. It is the address of the TCP connection:
		// X-Forwarded-For is written by the client, and an attacker would send a new value on every request.
		registry.addInterceptor(new RateLimitInterceptor(properties.auth().newLimiter(),
				HttpServletRequest::getRemoteAddr)).addPathPatterns("/api/auth/**");
		// Spring Security has already authenticated the request: the key is the token owner.
		registry.addInterceptor(new RateLimitInterceptor(properties.ai().newLimiter(),
				request -> request.getUserPrincipal().getName())).addPathPatterns("/api/ai/**");
	}
}
