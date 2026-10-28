package com.Unnia.identity.domain.model;

public enum Role {
	NGO,
	VOLUNTEER,
	/** Equipe da plataforma. Nunca é criado por cadastro público, apenas por seed/configuração. */
	ADMIN
}
