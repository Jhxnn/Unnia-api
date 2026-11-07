package com.Unnia.identity.infrastructure.security;

import com.Unnia.shared.infrastructure.ratelimit.RateLimit;
import com.Unnia.shared.infrastructure.ratelimit.RateLimitPolicy;
import com.Unnia.shared.infrastructure.ratelimit.RateLimiter;
import com.Unnia.shared.infrastructure.ratelimit.RateLimiter.Decision;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Limita a taxa de requisições. Roda antes da autenticação para recusar o excesso sem consultar o
 * banco: as rotas de autenticação são limitadas por IP e as demais por conta (lida do JWT, só pela
 * assinatura) ou, se anônimas, por IP.
 * <p>
 * O IP é o {@code remoteAddr}. Atrás de um proxy reverso, configure
 * {@code server.forward-headers-strategy}; ativá-lo sem proxy permitiria forjar o IP.
 */
class RateLimitFilter extends OncePerRequestFilter {

	private static final Set<String> AUTH_POSTS = Set.of("/api/auth/login", "/api/auth/refresh",
			"/api/auth/register/ngo", "/api/auth/register/volunteer", "/api/auth/verify-email",
			"/api/auth/resend-verification");
	private static final String AUTH_PASSWORD = "/api/auth/password";
	private static final String BEARER = "Bearer ";

	private final RateLimiter limiter;
	private final RateLimitPolicy policy;
	private final JwtService jwt;

	RateLimitFilter(RateLimiter limiter, RateLimitPolicy policy, JwtService jwt) {
		this.limiter = limiter;
		this.policy = policy;
		this.jwt = jwt;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (!policy.enabled()) {
			chain.doFilter(request, response);
			return;
		}

		boolean sensitive = isSensitive(request);
		RateLimit limit = sensitive ? policy.auth() : policy.general();
		String key = sensitive ? "auth:ip:" + request.getRemoteAddr() : "api:" + subject(request);
		Decision decision = limiter.tryAcquire(key, limit);

		response.setHeader("X-RateLimit-Limit", String.valueOf(limit.capacity()));
		response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
		if (decision.allowed()) {
			chain.doFilter(request, response);
			return;
		}
		reject(response, Math.max(1, (long) Math.ceil(decision.retryAfter().toMillis() / 1000.0)));
	}

	private boolean isSensitive(HttpServletRequest request) {
		String path = request.getRequestURI();
		return ("POST".equals(request.getMethod()) && AUTH_POSTS.contains(path))
				|| ("PUT".equals(request.getMethod()) && AUTH_PASSWORD.equals(path));
	}

	private String subject(HttpServletRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER)) {
			var user = jwt.parse(header.substring(BEARER.length()));
			if (user.isPresent()) {
				return "account:" + user.get().accountId().value();
			}
		}
		return "ip:" + request.getRemoteAddr();
	}

	private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
		response.setStatus(429);
		response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
		response.setContentType("application/problem+json");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Too Many Requests\",\"status\":429,"
				+ "\"detail\":\"Muitas requisições. Tente novamente em " + retryAfterSeconds + " segundos.\"}");
	}
}
