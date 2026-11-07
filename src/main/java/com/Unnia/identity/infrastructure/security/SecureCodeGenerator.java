package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.application.port.CodeGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
class SecureCodeGenerator implements CodeGenerator {

	private final SecureRandom random = new SecureRandom();

	@Override
	public String newCode() {
		return String.format("%06d", random.nextInt(1_000_000));
	}
}
