-- Migration V9: Add check_in_tokens table, FK on reservations, and drop legacy checkincode column.

CREATE TABLE check_in_tokens (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    event_id uuid NOT NULL,
    token character varying(255) NOT NULL,
    CONSTRAINT check_in_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT check_in_tokens_token_key UNIQUE (token),
    CONSTRAINT check_in_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT check_in_tokens_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

ALTER TABLE reservations ADD COLUMN check_in_token_id uuid NULL;
ALTER TABLE reservations ADD CONSTRAINT reservations_check_in_token_id_fkey
    FOREIGN KEY (check_in_token_id) REFERENCES check_in_tokens(id) ON DELETE SET NULL;

ALTER TABLE reservations DROP COLUMN checkincode;
