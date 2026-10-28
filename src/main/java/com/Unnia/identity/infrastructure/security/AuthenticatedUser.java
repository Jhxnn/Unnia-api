package com.Unnia.identity.infrastructure.security;

import com.Unnia.identity.domain.model.Role;
import com.Unnia.shared.domain.id.AccountId;

/** Usuário autenticado na requisição corrente; disponível nos controllers via {@code @AuthenticationPrincipal}. */
public record AuthenticatedUser(AccountId accountId, Role role) {
}
