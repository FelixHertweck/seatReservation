CREATE TABLE email_cooldowns (
    id uuid NOT NULL,
    purpose character varying(64) NOT NULL,
    cooldown_key character varying(255) NOT NULL,
    last_sent_at timestamp(6) with time zone NOT NULL
);
ALTER TABLE ONLY email_cooldowns ADD CONSTRAINT email_cooldowns_pkey PRIMARY KEY (id);
ALTER TABLE ONLY email_cooldowns ADD CONSTRAINT email_cooldowns_purpose_key_unique UNIQUE (purpose, cooldown_key);
