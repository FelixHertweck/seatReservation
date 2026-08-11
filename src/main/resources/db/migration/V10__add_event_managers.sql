-- Migration V10: Add event_managers table, populate from events.manager_id, and rename events.manager_id to events.created_by_user_id.
-- Also adds location_managers table for direct multi-manager sharing of event locations, populated
-- from eventlocations.manager_id so existing single-manager locations keep their access, and renames
-- eventlocations.manager_id to eventlocations.created_by_user_id to match the events table.

CREATE TABLE event_managers (
    event_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT event_managers_pkey PRIMARY KEY (event_id, user_id),
    CONSTRAINT event_managers_event_id_fkey FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT event_managers_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO event_managers (event_id, user_id)
SELECT id, manager_id FROM events WHERE manager_id IS NOT NULL;

ALTER TABLE events RENAME COLUMN manager_id TO created_by_user_id;

CREATE TABLE location_managers (
    event_location_id uuid NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT location_managers_pkey PRIMARY KEY (event_location_id, user_id),
    CONSTRAINT location_managers_event_location_id_fkey FOREIGN KEY (event_location_id) REFERENCES eventlocations(id) ON DELETE CASCADE,
    CONSTRAINT location_managers_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO location_managers (event_location_id, user_id)
SELECT id, manager_id FROM eventlocations WHERE manager_id IS NOT NULL;

ALTER TABLE eventlocations RENAME COLUMN manager_id TO created_by_user_id;
