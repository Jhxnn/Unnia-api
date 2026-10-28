package com.Unnia.identity.application.fakes;

import com.Unnia.identity.application.port.PasswordEncoder;
import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.RawPassword;

public class FakePasswordEncoder implements PasswordEncoder {

	private static final String PREFIX = "enc:";

	@Override
	public PasswordHash encode(RawPassword rawPassword) {
		return PasswordHash.of(PREFIX + rawPassword.value());
	}

	@Override
	public boolean matches(String rawPassword, PasswordHash hash) {
		return hash.value().equals(PREFIX + rawPassword);
	}
}
