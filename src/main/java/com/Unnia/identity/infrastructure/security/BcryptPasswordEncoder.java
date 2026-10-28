package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.application.port.PasswordEncoder;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.RawPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BcryptPasswordEncoder implements PasswordEncoder {

	private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

	@Override
	public PasswordHash encode(RawPassword rawPassword) {
		return PasswordHash.of(bcrypt.encode(rawPassword.value()));
	}

	@Override
	public boolean matches(String rawPassword, PasswordHash hash) {
		return bcrypt.matches(rawPassword, hash.value());
	}
}
