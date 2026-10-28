package com.Unnia.identity.application.port;

import com.Unnia.identity.domain.model.PasswordHash;
import com.Unnia.identity.domain.model.RawPassword;

public interface PasswordEncoder {

	PasswordHash encode(RawPassword rawPassword);

	boolean matches(String rawPassword, PasswordHash hash);
}
