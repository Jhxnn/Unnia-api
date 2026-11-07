-- O e-mail de confirmação passou de link para código de 6 dígitos: limita as tentativas erradas por código.
-- Os links já enviados deixam de servir (o hash agora é calculado com a conta); basta pedir um código novo.
ALTER TABLE email_verification_tokens ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0;
