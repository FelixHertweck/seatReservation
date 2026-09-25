-- One-off catch-up script for databases that were built by Hibernate's `schema-management.strategy:
-- update` before Flyway was introduced and that are OLDER than the V1 baseline.
--
-- Run it ONCE, via run.sh in this folder (which makes a backup first), BEFORE the first start
-- with Flyway enabled.
--
-- Afterwards the database matches V1__baseline.sql and Flyway (baseline-version=1) skips V1.
-- Databases that are already at the V1 schema do not need this script; running it again is a no-op.
-- Back up the database first.

BEGIN;

-- Tables that older versions did not have yet -------------------------------------------------

CREATE TABLE IF NOT EXISTS email_seat_map_tokens (
    created_at timestamp(6) with time zone NOT NULL,
    event_id bigint NOT NULL,
    expiration_time timestamp(6) with time zone NOT NULL,
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    token character varying(64) NOT NULL
);
CREATE SEQUENCE IF NOT EXISTS email_seat_map_tokens_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS email_seat_map_token_seats (
    token_id bigint NOT NULL,
    seat_number character varying(255)
);

CREATE TABLE IF NOT EXISTS event_location_areas (
    event_location_id bigint,
    id bigint NOT NULL,
    name character varying(255)
);
CREATE SEQUENCE IF NOT EXISTS event_location_areas_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS event_location_area_boundary (
    sort_order integer NOT NULL,
    xcoordinate integer,
    ycoordinate integer,
    area_id bigint NOT NULL,
    CONSTRAINT event_location_area_boundary_sort_order_check CHECK ((sort_order >= 0))
);

CREATE TABLE IF NOT EXISTS event_location_entrances (
    event_location_id bigint,
    id bigint NOT NULL,
    name character varying(255)
);
CREATE SEQUENCE IF NOT EXISTS event_location_entrances_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS event_supervisors (
    event_id bigint NOT NULL,
    user_id bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS login_attempts (
    successful boolean NOT NULL,
    attempttime timestamp(6) with time zone NOT NULL,
    id bigint NOT NULL,
    user_id bigint,
    username character varying(255) NOT NULL
);
CREATE SEQUENCE IF NOT EXISTS login_attempts_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS outbound_emails (
    attempts integer NOT NULL,
    max_attempts integer NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id bigint NOT NULL,
    next_attempt_at timestamp(6) with time zone NOT NULL,
    sent_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone NOT NULL,
    status character varying(16) NOT NULL,
    subject character varying(1024) NOT NULL,
    last_error character varying(2048),
    html_body oid NOT NULL,
    CONSTRAINT outbound_emails_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[])))
);
CREATE SEQUENCE IF NOT EXISTS outbound_emails_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS outbound_email_attachments (
    email_id bigint NOT NULL,
    id bigint NOT NULL,
    content_id character varying(255),
    content_type character varying(255) NOT NULL,
    file_name character varying(255) NOT NULL,
    data oid NOT NULL
);
CREATE SEQUENCE IF NOT EXISTS outbound_email_attachments_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS outbound_email_bcc (
    email_id bigint NOT NULL,
    address character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS outbound_email_cc (
    email_id bigint NOT NULL,
    address character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS outbound_email_recipients (
    email_id bigint NOT NULL,
    address character varying(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    createdat timestamp(6) with time zone,
    expiresat timestamp(6) with time zone,
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    tokenhash character varying(255)
);
CREATE SEQUENCE IF NOT EXISTS refresh_tokens_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS webauthn_credentials (
    counter bigint NOT NULL,
    createdat timestamp(6) with time zone,
    id bigint NOT NULL,
    lastusedat timestamp(6) with time zone,
    publickeyalgorithm bigint NOT NULL,
    user_id bigint NOT NULL,
    aaguid uuid,
    credentialid character varying(1024) NOT NULL,
    label character varying(255),
    publickey bytea NOT NULL
);
CREATE SEQUENCE IF NOT EXISTS webauthn_credentials_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

-- Renamed markers table -----------------------------------------------------------------------

DO $$
BEGIN
    IF to_regclass('eventlocationsmarkers') IS NOT NULL AND to_regclass('event_location_markers') IS NULL THEN
        ALTER TABLE eventlocationsmarkers RENAME TO event_location_markers;
        ALTER SEQUENCE eventlocationsmarkers_seq RENAME TO event_location_markers_seq;
        ALTER TABLE event_location_markers RENAME CONSTRAINT eventlocationsmarkers_pkey TO event_location_markers_pkey;
    END IF;
END $$;

-- Added columns -------------------------------------------------------------------------------

ALTER TABLE events ADD COLUMN IF NOT EXISTS remindersenddate timestamp(6) with time zone;
ALTER TABLE events ADD COLUMN IF NOT EXISTS remindersent boolean NOT NULL DEFAULT false;
ALTER TABLE events ALTER COLUMN remindersent DROP DEFAULT;
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS checkincode varchar(255);
ALTER TABLE reservations ADD COLUMN IF NOT EXISTS livestatus varchar(255);
ALTER TABLE seats ADD COLUMN IF NOT EXISTS area_id bigint;
ALTER TABLE seats ADD COLUMN IF NOT EXISTS entrance_id bigint;
ALTER TABLE seats ALTER COLUMN xcoordinate DROP NOT NULL;
ALTER TABLE seats ALTER COLUMN ycoordinate DROP NOT NULL;

-- Constraints and indexes (each only if missing, so the script is re-runnable) ----------------

DO $$
DECLARE
    c record;
BEGIN
    FOR c IN
        SELECT * FROM (VALUES
            ('email_seat_map_token_seats', 'email_seat_map_token_seats_token_id_seat_number_key', 'UNIQUE (token_id, seat_number)'),
            ('email_seat_map_tokens', 'email_seat_map_tokens_pkey', 'PRIMARY KEY (id)'),
            ('email_seat_map_tokens', 'email_seat_map_tokens_token_key', 'UNIQUE (token)'),
            ('event_location_area_boundary', 'event_location_area_boundary_pkey', 'PRIMARY KEY (sort_order, area_id)'),
            ('event_location_areas', 'event_location_areas_pkey', 'PRIMARY KEY (id)'),
            ('event_location_entrances', 'event_location_entrances_pkey', 'PRIMARY KEY (id)'),
            ('event_supervisors', 'event_supervisors_pkey', 'PRIMARY KEY (event_id, user_id)'),
            ('login_attempts', 'login_attempts_pkey', 'PRIMARY KEY (id)'),
            ('outbound_email_attachments', 'outbound_email_attachments_pkey', 'PRIMARY KEY (id)'),
            ('outbound_emails', 'outbound_emails_pkey', 'PRIMARY KEY (id)'),
            ('refresh_tokens', 'refresh_tokens_pkey', 'PRIMARY KEY (id)'),
            ('webauthn_credentials', 'webauthn_credentials_credentialid_key', 'UNIQUE (credentialid)'),
            ('webauthn_credentials', 'webauthn_credentials_pkey', 'PRIMARY KEY (id)'),
            ('reservations', 'reservations_event_id_user_id_checkincode_key', 'UNIQUE (event_id, user_id, checkincode)'),
            ('reservations', 'reservations_livestatus_check', $c$CHECK (((livestatus)::text = ANY ((ARRAY['CHECKED_IN'::character varying, 'CANCELLED'::character varying, 'NO_SHOW'::character varying])::text[])))$c$),

            ('refresh_tokens', 'fk1lih5y2npsf8u5o3vhdb9y0os', 'FOREIGN KEY (user_id) REFERENCES users(id)'),
            ('event_supervisors', 'fk3aoscdyd7pr6u826dxmj5eq34', 'FOREIGN KEY (event_id) REFERENCES events(id)'),
            ('event_location_entrances', 'fk5d8980bb9w8gflo6kuaulslmc', 'FOREIGN KEY (event_location_id) REFERENCES eventlocations(id)'),
            ('webauthn_credentials', 'fk61k8kijke2qqqpsrg65qjwcie', 'FOREIGN KEY (user_id) REFERENCES users(id)'),
            ('outbound_email_cc', 'fk9iqj75se6yd752rpnvh444t2y', 'FOREIGN KEY (email_id) REFERENCES outbound_emails(id)'),
            ('seats', 'fkewopqcg6ejtw7w1cr72h81uic', 'FOREIGN KEY (entrance_id) REFERENCES event_location_entrances(id)'),
            ('event_location_area_boundary', 'fkfho28q7mc9mn6ku9vaah1ecad', 'FOREIGN KEY (area_id) REFERENCES event_location_areas(id)'),
            ('event_supervisors', 'fkh5dc0cko6fxnrh14ab4u9hgsq', 'FOREIGN KEY (user_id) REFERENCES users(id)'),
            ('email_seat_map_tokens', 'fki7ok9vlqtnek7sfpejikoppbm', 'FOREIGN KEY (event_id) REFERENCES events(id)'),
            ('outbound_email_bcc', 'fkkbqiu53cwbwt37ius2rmdgrn3', 'FOREIGN KEY (email_id) REFERENCES outbound_emails(id)'),
            ('outbound_email_attachments', 'fkodwuh62l6gv1jsvllvodtwwwn', 'FOREIGN KEY (email_id) REFERENCES outbound_emails(id)'),
            ('outbound_email_recipients', 'fkoia5cdiai43feapmxw3fh7aqn', 'FOREIGN KEY (email_id) REFERENCES outbound_emails(id)'),
            ('email_seat_map_tokens', 'fkomqn7ooasx3jq8oo2hs3lw2vf', 'FOREIGN KEY (user_id) REFERENCES users(id)'),
            ('email_seat_map_token_seats', 'fkpywj3cn0csx3jpr0sneprc7wo', 'FOREIGN KEY (token_id) REFERENCES email_seat_map_tokens(id)'),
            ('seats', 'fkqghl2hgr3uhl9krv29b4xeqcq', 'FOREIGN KEY (area_id) REFERENCES event_location_areas(id)'),
            ('event_location_areas', 'fkscw524e3yojfvg7ed2chgccxr', 'FOREIGN KEY (event_location_id) REFERENCES eventlocations(id)'),
            ('login_attempts', 'fktg9vhke4mlf5vij2rcvfk2dg2', 'FOREIGN KEY (user_id) REFERENCES users(id)')
        ) AS t(tbl, name, definition)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = c.name AND conrelid = to_regclass(c.tbl)) THEN
            EXECUTE format('ALTER TABLE ONLY %I ADD CONSTRAINT %I %s', c.tbl, c.name, c.definition);
        END IF;
    END LOOP;
END $$;

CREATE INDEX IF NOT EXISTS idx_expiration_time ON email_seat_map_tokens USING btree (expiration_time);
CREATE INDEX IF NOT EXISTS idx_outbound_email_next_attempt ON outbound_emails USING btree (next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_outbound_email_status ON outbound_emails USING btree (status);
CREATE INDEX IF NOT EXISTS idx_token ON email_seat_map_tokens USING btree (token);
CREATE INDEX IF NOT EXISTS idx_user_event ON email_seat_map_tokens USING btree (user_id, event_id);

COMMIT;
