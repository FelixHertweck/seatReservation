-- No known code path ever writes a null reservation status, but the column
-- allowed it. Backfill defensively before enforcing NOT NULL so this
-- migration cannot fail on legacy/malformed rows.
UPDATE reservations SET status = 'RESERVED' WHERE status IS NULL;

ALTER TABLE reservations ALTER COLUMN status SET NOT NULL;
