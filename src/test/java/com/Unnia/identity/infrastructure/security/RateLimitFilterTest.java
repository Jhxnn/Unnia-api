package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.application.MutableClock;
import com.Unnia.shared.domain.id.AccountId;
import com.Unnia.shared.infrastructure.ratelimit.InMemoryRateLimiter;
import com.Unnia.shared.infrastructure.ratelimit.RateLimit;
import com.Unnia.shared.infrastructure.ratelimit.RateLimitPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

	private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");

	private MutableClock clock;
	private JwtService jwt;
	private RateLimitFilter filter;
	private final AtomicInteger passed = new AtomicInteger();

	@BeforeEach
	void setUp() {
		clock = new MutableClock(NOW);
		jwt = new JwtService("a-secret-with-clearly-more-than-32-chars", Duration.ofMinutes(15), clock);
		filter = new RateLimitFilter(new InMemoryRateLimiter(clock),
				new RateLimitPolicy(true, RateLimit.perMinute(3), RateLimit.perMinute(5)), jwt);
	}

	private MockHttpServletResponse call(String method, String path, String ip, String token) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.setRemoteAddr(ip);
		if (token != null) {
			request.addHeader("Authorization", "Bearer " + token);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, (req, res) -> passed.incrementAndGet());
		return response;
	}

	private String tokenOf(AccountId account) {
		return jwt.issue(account, Role.VOLUNTEER).value();
	}

	@Test
	void authenticationRoutesAreLimitedPerIpAndAnswerTooManyRequests() throws Exception {
		for (int i = 0; i < 3; i++) {
			assertThat(call("POST", "/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(200);
		}

		MockHttpServletResponse blocked = call("POST", "/api/auth/login", "10.0.0.1", null);

		assertThat(blocked.getStatus()).isEqualTo(429);
		assertThat(blocked.getHeader("Retry-After")).isEqualTo("20");
		assertThat(blocked.getContentType()).startsWith("application/problem+json");
		assertThat(blocked.getContentAsString()).contains("\"status\":429").contains("Tente novamente em 20 segundos");
		assertThat(passed.get()).isEqualTo(3);
	}

	@Test
	void loginRefreshRegisterAndPasswordChangeShareTheAuthBucket() throws Exception {
		call("POST", "/api/auth/login", "10.0.0.1", null);
		call("POST", "/api/auth/refresh", "10.0.0.1", null);
		call("POST", "/api/auth/register/ngo", "10.0.0.1", null);

		assertThat(call("POST", "/api/auth/register/volunteer", "10.0.0.1", null).getStatus()).isEqualTo(429);
		assertThat(call("PUT", "/api/auth/password", "10.0.0.1", null).getStatus()).isEqualTo(429);
	}

	@Test
	void differentIpsHaveTheirOwnAuthBucket() throws Exception {
		for (int i = 0; i < 3; i++) {
			call("POST", "/api/auth/login", "10.0.0.1", null);
		}

		assertThat(call("POST", "/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(429);
		assertThat(call("POST", "/api/auth/login", "10.0.0.2", null).getStatus()).isEqualTo(200);
	}

	@Test
	void onlyTheListedMethodsAndPathsCountAsSensitive() throws Exception {
		for (int i = 0; i < 6; i++) {
			call("POST", "/api/auth/logout", "10.0.0.1", null);
		}
		// logout, GET em /login e outras rotas caem no limite geral (5), não no de autenticação (3)
		assertThat(passed.get()).isEqualTo(5);
	}

	@Test
	void generalRoutesAreLimitedPerIpWhenAnonymous() throws Exception {
		for (int i = 0; i < 5; i++) {
			assertThat(call("GET", "/api/events", "10.0.0.1", null).getStatus()).isEqualTo(200);
		}

		assertThat(call("GET", "/api/events", "10.0.0.1", null).getStatus()).isEqualTo(429);
		assertThat(call("GET", "/api/events", "10.0.0.2", null).getStatus()).isEqualTo(200);
	}

	@Test
	void authenticatedRequestsAreLimitedPerAccountRegardlessOfIp() throws Exception {
		String token = tokenOf(AccountId.generate());
		for (int i = 0; i < 5; i++) {
			call("GET", "/api/posts/feed", "10.0.0." + i, token);
		}

		assertThat(call("GET", "/api/posts/feed", "10.9.9.9", token).getStatus()).isEqualTo(429);
	}

	@Test
	void accountsBehindTheSameIpDoNotShareTheirLimit() throws Exception {
		String busy = tokenOf(AccountId.generate());
		String quiet = tokenOf(AccountId.generate());
		for (int i = 0; i < 5; i++) {
			call("GET", "/api/posts/feed", "10.0.0.1", busy);
		}

		assertThat(call("GET", "/api/posts/feed", "10.0.0.1", busy).getStatus()).isEqualTo(429);
		assertThat(call("GET", "/api/posts/feed", "10.0.0.1", quiet).getStatus()).isEqualTo(200);
	}

	@Test
	void anInvalidTokenFallsBackToTheIpBucket() throws Exception {
		for (int i = 0; i < 5; i++) {
			call("GET", "/api/events", "10.0.0.1", "token-invalido");
		}

		assertThat(call("GET", "/api/events", "10.0.0.1", null).getStatus()).isEqualTo(429);
	}

	@Test
	void authenticationAndGeneralBucketsAreIndependent() throws Exception {
		for (int i = 0; i < 3; i++) {
			call("POST", "/api/auth/login", "10.0.0.1", null);
		}

		assertThat(call("POST", "/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(429);
		assertThat(call("GET", "/api/events", "10.0.0.1", null).getStatus()).isEqualTo(200);
	}

	@Test
	void requestsAreAcceptedAgainAsTheBucketRefills() throws Exception {
		for (int i = 0; i < 3; i++) {
			call("POST", "/api/auth/login", "10.0.0.1", null);
		}
		assertThat(call("POST", "/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(429);

		clock.advance(Duration.ofSeconds(20));

		assertThat(call("POST", "/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(200);
	}

	@Test
	void reportsTheLimitAndWhatIsLeftInHeaders() throws Exception {
		MockHttpServletResponse first = call("GET", "/api/events", "10.0.0.1", null);
		MockHttpServletResponse second = call("GET", "/api/events", "10.0.0.1", null);

		assertThat(first.getHeader("X-RateLimit-Limit")).isEqualTo("5");
		assertThat(first.getHeader("X-RateLimit-Remaining")).isEqualTo("4");
		assertThat(second.getHeader("X-RateLimit-Remaining")).isEqualTo("3");
	}

	@Test
	void whenDisabledNothingIsLimited() throws Exception {
		filter = new RateLimitFilter(new InMemoryRateLimiter(clock),
				new RateLimitPolicy(false, RateLimit.perMinute(1), RateLimit.perMinute(1)), jwt);

		for (int i = 0; i < 20; i++) {
			assertThat(call("POST", "/api/auth/login", "10.0.0.1", null).getStatus()).isEqualTo(200);
		}
		assertThat(passed.get()).isEqualTo(20);
	}
}
