package com.Unnia.identity.application.port;

public interface TokenHasher {

	/** Hash determinístico do token, que é o que se persiste; o token em si nunca é guardado. */
	String hash(String token);
}
