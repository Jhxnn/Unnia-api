-- Sessões renováveis: guarda apenas o hash do token entregue ao cliente.
-- Cada renovação gera um sucessor na mesma família; reusar um token já usado derruba a família.
CREATE TABLE refresh_tokens (
    id         UUID        PRIMARY KEY,
    account_id UUID        NOT NULL REFERENCES accounts (id),
    family_id  UUID        NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    issued_at  TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    version    BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_account ON refresh_tokens (account_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens (expires_at);
