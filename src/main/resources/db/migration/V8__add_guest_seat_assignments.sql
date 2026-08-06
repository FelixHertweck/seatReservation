CREATE TABLE guest_seat_assignments (
    id uuid NOT NULL,
    event_id uuid NOT NULL,
    seat_id uuid NOT NULL,
    guest_name character varying(255) NOT NULL,
    assigned_by_id uuid NOT NULL,
    assigned_at timestamp(6) with time zone NOT NULL
);
ALTER TABLE ONLY guest_seat_assignments
    ADD CONSTRAINT guest_seat_assignments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY guest_seat_assignments
    ADD CONSTRAINT guest_seat_assignments_event_seat_unique UNIQUE (event_id, seat_id);
ALTER TABLE ONLY guest_seat_assignments
    ADD CONSTRAINT guest_seat_assignments_event_fk FOREIGN KEY (event_id) REFERENCES events(id);
ALTER TABLE ONLY guest_seat_assignments
    ADD CONSTRAINT guest_seat_assignments_seat_fk FOREIGN KEY (seat_id) REFERENCES seats(id);
ALTER TABLE ONLY guest_seat_assignments
    ADD CONSTRAINT guest_seat_assignments_assigned_by_fk FOREIGN KEY (assigned_by_id) REFERENCES users(id);
