-- Adds Two-Factor Authentication (2FA) support: columns on users table and tables for backup codes & login challenges.

ALTER TABLE users ADD COLUMN two_factor_enabled boolean DEFAULT false NOT NULL;
-- TOTP and email are independent, equally valid factors -- both may be active at once.
ALTER TABLE users ADD COLUMN totp_enabled boolean DEFAULT false NOT NULL;
ALTER TABLE users ADD COLUMN email_enabled boolean DEFAULT false NOT NULL;
ALTER TABLE users ADD COLUMN two_factor_passkey_enabled boolean DEFAULT false NOT NULL;
-- Encrypted at rest (AES-256-GCM, see EncryptedStringConverter); wider than a raw base32 secret.
ALTER TABLE users ADD COLUMN totp_secret character varying(512);
-- Last TOTP time-step accepted for this user, to reject replay of an intercepted code.
ALTER TABLE users ADD COLUMN last_totp_step bigint;

CREATE TABLE two_factor_backup_codes (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    code_hash character varying(255) NOT NULL,
    used boolean DEFAULT false NOT NULL,
    created_at timestamp(6) with time zone NOT NULL
);

ALTER TABLE ONLY two_factor_backup_codes ADD CONSTRAINT two_factor_backup_codes_pkey PRIMARY KEY (id);
ALTER TABLE ONLY two_factor_backup_codes ADD CONSTRAINT two_factor_backup_codes_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
-- Postgres does not auto-index FK columns; every lookup here (countUnusedByUser, findUnusedByUser) filters by user_id.
CREATE INDEX idx_two_factor_backup_codes_user_id ON two_factor_backup_codes (user_id);

CREATE TABLE two_factor_challenges (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    challenge_token character varying(255) NOT NULL,
    email_code character varying(50),
    created_at timestamp(6) with time zone NOT NULL,
    expires_at timestamp(6) with time zone NOT NULL,
    used boolean DEFAULT false NOT NULL
);

ALTER TABLE ONLY two_factor_challenges ADD CONSTRAINT two_factor_challenges_pkey PRIMARY KEY (id);
ALTER TABLE ONLY two_factor_challenges ADD CONSTRAINT two_factor_challenges_token_key UNIQUE (challenge_token);
ALTER TABLE ONLY two_factor_challenges ADD CONSTRAINT two_factor_challenges_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
-- deleteByUser (setup/challenge/disable flows) filters by user_id; the token lookup itself is
-- already covered by the unique constraint above.
CREATE INDEX idx_two_factor_challenges_user_id ON two_factor_challenges (user_id);

-- Tracks 2FA verification attempts for rate limiting, keyed by user (not by challenge), so
-- issuing a fresh challenge does not reset an attacker's failed-attempt budget.
CREATE TABLE two_factor_attempts (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    attempt_time timestamp(6) with time zone NOT NULL,
    successful boolean NOT NULL
);

ALTER TABLE ONLY two_factor_attempts ADD CONSTRAINT two_factor_attempts_pkey PRIMARY KEY (id);
ALTER TABLE ONLY two_factor_attempts ADD CONSTRAINT two_factor_attempts_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
-- checkTwoFactorLockout's countFailedAttempts/getOldestFailedAttemptTime queries filter by
-- user_id and order by attempt_time on every 2FA verification.
CREATE INDEX idx_two_factor_attempts_user_id_attempt_time ON two_factor_attempts (user_id, attempt_time);
