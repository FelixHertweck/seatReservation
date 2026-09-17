-- Every reservation-creation path validates the seat before constructing the
-- Reservation and rejects the request if it is missing, so seat_id is never
-- actually null in practice. But seat_id was nullable since V1__baseline.sql,
-- so older data may still contain such rows. A reservation without a seat
-- reserves nothing, so it is already meaningless data - delete rather than
-- backfill with a synthetic seat, which would fabricate a reservation that
-- never happened (boxoffice_guest_info cascades on delete).
DELETE FROM reservations WHERE seat_id IS NULL;

ALTER TABLE reservations ALTER COLUMN seat_id SET NOT NULL;
