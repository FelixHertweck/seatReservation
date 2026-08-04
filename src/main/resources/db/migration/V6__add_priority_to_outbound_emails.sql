-- Adds priority column to outbound_emails to prioritize transactional emails (e.g. 2FA, password resets)
-- over bulk emails (e.g. event reminders).

ALTER TABLE outbound_emails ADD COLUMN priority character varying(32) DEFAULT 'TRANSACTIONAL' NOT NULL;

CREATE INDEX idx_outbound_email_status_priority_next_attempt ON outbound_emails USING btree (status, priority, next_attempt_at);
