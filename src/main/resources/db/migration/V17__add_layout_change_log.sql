CREATE TABLE layout_change_log (
    id uuid NOT NULL,
    batch_id uuid NOT NULL,
    sequence_no integer NOT NULL,
    event_location_id uuid NOT NULL,
    user_id uuid,
    entity_type character varying(32) NOT NULL,
    action character varying(16) NOT NULL,
    entity_id uuid,
    ref character varying(255),
    payload text,
    created_at timestamp(6) with time zone NOT NULL
);
ALTER TABLE ONLY layout_change_log ADD CONSTRAINT layout_change_log_pkey PRIMARY KEY (id);
CREATE INDEX idx_layout_change_log_batch ON layout_change_log (batch_id, sequence_no);
CREATE INDEX idx_layout_change_log_location ON layout_change_log (event_location_id);
