package com.Unnia.shared.infrastructure.ratelimit;

/**
 * Política de limites da API.
 *
 * @param auth    rotas de autenticação (login, refresh, cadastro, troca de senha), por IP
 * @param general demais rotas, por conta autenticada ou, se anônimo, por IP
 */
public record RateLimitPolicy(boolean enabled, RateLimit auth, RateLimit general) {
}
