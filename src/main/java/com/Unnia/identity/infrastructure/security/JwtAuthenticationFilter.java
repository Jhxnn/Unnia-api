package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.domain.model.Account;
import com.Unnia.identity.domain.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Autentica a requisição a partir do cabeçalho {@code Authorization: Bearer}. Confere a conta a
 * cada requisição, então suspender uma conta invalida seus tokens imediatamente.
 */
class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final JwtService jwt;
	private final AccountRepository accounts;

	JwtAuthenticationFilter(JwtService jwt, AccountRepository accounts) {
		this.jwt = jwt;
		this.accounts = accounts;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith(BEARER)) {
			jwt.parse(header.substring(BEARER.length())).filter(this::isStillActive).ifPresent(this::authenticate);
		}
		chain.doFilter(request, response);
	}

	private boolean isStillActive(AuthenticatedUser user) {
		return accounts.findById(user.accountId()).filter(Account::isActive).isPresent();
	}

	private void authenticate(AuthenticatedUser user) {
		var authentication = new UsernamePasswordAuthenticationToken(user, null,
				List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
