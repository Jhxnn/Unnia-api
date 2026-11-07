package com.Unnia.shared.infrastructure.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class RateLimitConfig {

	@Bean
	RateLimiter rateLimiter(Clock clock) {
		return new InMemoryRateLimiter(clock);
	}

	@Bean
	RateLimitPolicy rateLimitPolicy(@Value("${unnia.rate-limit.enabled:true}") boolean enabled,
			@Value("${unnia.rate-limit.auth-per-minute:10}") int authPerMinute,
			@Value("${unnia.rate-limit.general-per-minute:120}") int generalPerMinute) {
		return new RateLimitPolicy(enabled, RateLimit.perMinute(authPerMinute), RateLimit.perMinute(generalPerMinute));
	}
}
