-- Shared, passwordless system user for walk-in box office reservations (no real account).
INSERT INTO users (
    id, username, firstname, lastname, passwordhash, passwordsalt, email,
    emailverified, emailverificationsent
) VALUES (
    gen_random_uuid(), 'boxoffice', NULL, NULL, NULL, NULL, NULL,
    false, false
);

-- Maps a box office guest reservation to the guest's name.
CREATE TABLE boxoffice_guest_info (
    id uuid NOT NULL,
    reservation_id uuid NOT NULL,
    guestname character varying(255) NOT NULL,
    CONSTRAINT boxoffice_guest_info_pkey PRIMARY KEY (id),
    CONSTRAINT boxoffice_guest_info_reservation_id_key UNIQUE (reservation_id),
    CONSTRAINT boxoffice_guest_info_reservation_id_fkey FOREIGN KEY (reservation_id)
        REFERENCES reservations(id) ON DELETE CASCADE
);
