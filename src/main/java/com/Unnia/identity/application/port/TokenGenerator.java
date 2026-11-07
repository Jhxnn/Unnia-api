package com.Unnia.identity.application.port;

public interface TokenGenerator {

	/** Gera um token opaco, imprevisível, para entregar ao cliente. */
	String newToken();
}
