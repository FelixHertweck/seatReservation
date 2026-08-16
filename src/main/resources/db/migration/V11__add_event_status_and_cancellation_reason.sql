-- Migration V11: Add status and cancellationreason to events table

ALTER TABLE events ADD COLUMN status character varying(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE events ADD COLUMN cancellationreason text;
