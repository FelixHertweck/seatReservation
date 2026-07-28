-- Adds the password_reset_tokens table backing the password-reset feature. Mirrors the shape of
-- email_verification (one active token per user, unique token, FK to users), created directly
-- with a UUID primary key since this table is new after the V2 UUID migration.

CREATE TABLE password_reset_tokens (
    expiration_time timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    user_id uuid,
    token character varying(255) NOT NULL
);

ALTER TABLE ONLY password_reset_tokens ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);
ALTER TABLE ONLY password_reset_tokens ADD CONSTRAINT password_reset_tokens_token_key UNIQUE (token);
ALTER TABLE ONLY password_reset_tokens ADD CONSTRAINT password_reset_tokens_user_id_key UNIQUE (user_id);
ALTER TABLE ONLY password_reset_tokens ADD CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);
