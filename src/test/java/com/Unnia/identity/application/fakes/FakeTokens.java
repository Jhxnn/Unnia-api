package com.Unnia.identity.application.fakes;

import com.Unnia.identity.application.port.TokenGenerator;
import com.Unnia.identity.application.port.TokenHasher;

/** Gerador determinístico ("token-1", "token-2", ...) e hasher trivial ("hash:" + token) para os testes. */
public class FakeTokens implements TokenGenerator, TokenHasher {

	private int counter;

	@Override
	public String newToken() {
		return "token-" + (++counter);
	}

	@Override
	public String hash(String token) {
		return "hash:" + token;
	}
}
