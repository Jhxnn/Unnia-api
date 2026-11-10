package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.domain.repository.AccountRepository;
import com.Unnia.shared.infrastructure.ratelimit.RateLimitPolicy;
import com.Unnia.shared.infrastructure.ratelimit.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt, AccountRepository accounts,
			RateLimiter rateLimiter, RateLimitPolicy rateLimitPolicy) throws Exception {
		// instanciados aqui, e não como @Component, para rodar só dentro da cadeia do Spring Security
		JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwt, accounts);
		RateLimitFilter rateLimitFilter = new RateLimitFilter(rateLimiter, rateLimitPolicy, jwt);
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(e -> e
						.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
						// responde 403 direto, sem sendError: o redirecionamento para /error viraria 401
						.accessDeniedHandler((request, response, denied) -> response.setStatus(HttpStatus.FORBIDDEN.value())))
				.authorizeHttpRequests(auth -> auth
						// o encerramento de uma conexão SSE volta ao servlet como despacho assíncrono, já autenticado na abertura
						.dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
						.requestMatchers("/error").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
								"/api/auth/register/ngo", "/api/auth/register/volunteer", "/api/auth/verify-email",
								"/api/auth/resend-verification")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/api/events/mine", "/api/events/registered").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/events", "/api/events/*", "/api/ngos", "/api/ngos/*",
								"/api/volunteers/*")
						.permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				// o limite vem primeiro, para recusar o excesso antes de qualquer consulta ao banco
				.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	/**
	 * A autenticação é feita pelo filtro JWT, não por usuário e senha do Spring Security. Declarar
	 * este bean impede o Spring Boot de criar o usuário padrão com senha gerada no log.
	 */
	@Bean
	UserDetailsService noUserDetailsService() {
		return username -> {
			throw new UsernameNotFoundException("Autenticação por usuário e senha não é usada");
		};
	}
}
