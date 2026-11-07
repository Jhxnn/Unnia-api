-- Confirmação de e-mail: a conta só entra depois de provar que a caixa de entrada existe.
ALTER TABLE accounts ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- as contas anteriores a esta regra continuam valendo
UPDATE accounts SET email_verified = TRUE;

-- Guarda apenas o hash do token enviado no link; só um link pendente vale por vez.
CREATE TABLE email_verification_tokens (
    id         UUID        PRIMARY KEY,
    account_id UUID        NOT NULL REFERENCES accounts (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    issued_at  TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ
);
CREATE INDEX idx_email_verification_account ON email_verification_tokens (account_id, issued_at DESC);
