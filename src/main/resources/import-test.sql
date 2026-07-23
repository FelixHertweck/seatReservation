-- Test-specific minimal data import
-- This file contains only the minimal data needed for tests
--
-- IDs are fixed UUIDs of the form 00000000-0000-0000-0000-00000000000N, matching
-- de.felixhertweck.seatreservation.testutil.TestIds.id(N) in the test sources, so Java test
-- code can reference these seeded rows by the same small integer used here.

-- Insert admin user for tests
INSERT INTO users (id, username, email, passwordHash, passwordSalt, firstname, lastname, emailVerified) VALUES ('00000000-0000-0000-0000-000000000001', 'admin', 'admin@localhost', '$2a$12$G0LZJi5jGdl5wqspjaVYN.eXdZcZ3X9cMny/3m8mRM3vK/5Yf6TE6', 'Salt', 'Admin', 'User', true);

-- Assign ADMIN tags to admin user
INSERT INTO user_tags (user_id, tags) VALUES ('00000000-0000-0000-0000-000000000001', 'superuser');
INSERT INTO user_tags (user_id, tags) VALUES ('00000000-0000-0000-0000-000000000001', 'demouser');

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role) VALUES ('00000000-0000-0000-0000-000000000001', 'ADMIN');

-- Insert manager user for tests
INSERT INTO users (id, username, email, passwordHash, passwordSalt, firstname, lastname, emailVerified) VALUES ('00000000-0000-0000-0000-000000000002', 'manager', 'manager@localhost', '$2a$12$R.1dngJ7Ma4DEVU8L7Tjlek2AnE4.SPIK/BHynpwoatUim9qeOoP2', 'Salt', 'Event', 'Manager', true);

-- Assign ADMIN tags to manager user
INSERT INTO user_tags (user_id, tags) VALUES ('00000000-0000-0000-0000-000000000002', 'manageruser');
INSERT INTO user_tags (user_id, tags) VALUES ('00000000-0000-0000-0000-000000000002', 'demouser');

-- Assign MANAGER role to manager user
INSERT INTO user_roles (user_id, role) VALUES ('00000000-0000-0000-0000-000000000002', 'MANAGER');

-- Insert regular user for tests
INSERT INTO users (id, username, email, passwordHash, passwordSalt, firstname, lastname, emailVerified) VALUES ('00000000-0000-0000-0000-000000000003', 'user', 'user@localhost', '$2a$12$arzn9PpRswUrgIw0xaXoM.Y2PeX8m9OKKEGggA07bUXm/O./85WPa', 'Salt', 'Regular', 'User', true);

-- Assign ADMIN tags to regular user
INSERT INTO user_tags (user_id, tags) VALUES ('00000000-0000-0000-0000-000000000003', 'normaluser');
INSERT INTO user_tags (user_id, tags) VALUES ('00000000-0000-0000-0000-000000000003', 'demouser');

-- Assign USER role to regular user
INSERT INTO user_roles (user_id, role) VALUES ('00000000-0000-0000-0000-000000000003', 'USER');

-- Insert supervisor user for tests
INSERT INTO users (id, username, email, passwordHash, passwordSalt, firstname, lastname, emailVerified) VALUES ('00000000-0000-0000-0000-000000000004', 'supervisor', 'supervisor@localhost', '$2a$12$arzn9PpRswUrgIw0xaXoM.Y2PeX8m9OKKEGggA07bUXm/O./85WPa', 'Salt', 'Supervisor', 'User', true);

-- Assign ADMIN tags to supervisor user
INSERT INTO user_tags (user_id, tags) VALUES ('00000000-0000-0000-0000-000000000004', 'supervisoruser');
INSERT INTO user_tags (user_id, tags) VALUES ('00000000-0000-0000-0000-000000000004', 'demouser');

-- Assign USER role to regular user
INSERT INTO user_roles (user_id, role) VALUES ('00000000-0000-0000-0000-000000000003', 'SUPERVISOR');

-- Insert a minimal event location for tests
INSERT INTO eventlocations (id, name, address, manager_id) VALUES ('00000000-0000-0000-0000-000000000001', 'Test Hall', 'Teststraße 1, 12345 Teststadt', '00000000-0000-0000-0000-000000000002');

-- Insert a minimal event for tests
INSERT INTO events (id, name, startTime, endTime, bookingStartTime, bookingDeadline, reminderSent, event_location_id, manager_id) VALUES ('00000000-0000-0000-0000-000000000001', 'Test Concert', '2024-12-31 19:00:00', '2024-12-31 21:00:00', '2024-12-12 17:00:00', '2026-12-12 17:00:00', false, '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002');

-- Assign supervisor user (id 4) to the event (id 1)
INSERT INTO event_supervisors (event_id, user_id) VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000004');

-- Insert areas (referenced by seats via area_id below)
INSERT INTO event_location_areas (id, event_location_id, name) VALUES
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Ground Floor'),
('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Balcony');

-- Insert entrances (referenced by seats via entrance_id below)
INSERT INTO event_location_entrances (id, event_location_id, name) VALUES
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'Entrance A'),
('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'Entrance B');

-- Insert seats, split into two areas (Ground Floor on the ground floor, Balcony above)
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate, seatRow, entrance_id, area_id) VALUES
('00000000-0000-0000-0000-000000000001', '1', '00000000-0000-0000-0000-000000000001', 1, 1, 'Row 1', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000002', '2', '00000000-0000-0000-0000-000000000001', 2, 1, 'Row 1', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000003', '3', '00000000-0000-0000-0000-000000000001', 3, 1, 'Row 1', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000004', '4', '00000000-0000-0000-0000-000000000001', 4, 1, 'Row 1', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000005', '5', '00000000-0000-0000-0000-000000000001', 1, 2, 'Balcony 1', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'),
('00000000-0000-0000-0000-000000000006', '6', '00000000-0000-0000-0000-000000000001', 2, 2, 'Balcony 1', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'),
('00000000-0000-0000-0000-000000000007', '7', '00000000-0000-0000-0000-000000000001', 3, 2, 'Balcony 1', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'),
('00000000-0000-0000-0000-000000000008', '8', '00000000-0000-0000-0000-000000000001', 4, 2, 'Balcony 1', '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002');
