package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.domain.repository.AccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt, AccountRepository accounts)
			throws Exception {
		// instanciado aqui, e não como @Component, para rodar só dentro da cadeia do Spring Security
		JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwt, accounts);
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register/ngo",
								"/api/auth/register/volunteer")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/api/events/mine", "/api/events/registered").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/events", "/api/events/*", "/api/ngos/*",
								"/api/volunteers/*")
						.permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
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
