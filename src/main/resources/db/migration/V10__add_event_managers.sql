-- Migration V10: Add event_managers table, populate from events.manager_id, and rename events.manager_id to events.created_by_user_id.

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
