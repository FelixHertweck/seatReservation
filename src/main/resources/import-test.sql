-- Test-specific minimal data import
-- This file contains only the minimal data needed for tests

-- Insert admin user for tests
INSERT INTO users (id, username, email, passwordHash, passwordSalt, firstname, lastname, emailVerified) VALUES (1, 'admin', 'admin@localhost', '$2a$12$G0LZJi5jGdl5wqspjaVYN.eXdZcZ3X9cMny/3m8mRM3vK/5Yf6TE6', 'Salt', 'Admin', 'User', true);

-- Assign ADMIN tags to admin user
INSERT INTO user_tags (user_id, tags) VALUES (1, 'superuser');
INSERT INTO user_tags (user_id, tags) VALUES (1, 'demouser');

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN');

-- Insert manager user for tests
INSERT INTO users (id, username, email, passwordHash, passwordSalt, firstname, lastname, emailVerified) VALUES (2, 'manager', 'manager@localhost', '$2a$12$R.1dngJ7Ma4DEVU8L7Tjlek2AnE4.SPIK/BHynpwoatUim9qeOoP2', 'Salt', 'Event', 'Manager', true);

-- Assign ADMIN tags to manager user
INSERT INTO user_tags (user_id, tags) VALUES (2, 'manageruser');
INSERT INTO user_tags (user_id, tags) VALUES (2, 'demouser');

-- Assign MANAGER role to manager user
INSERT INTO user_roles (user_id, role) VALUES (2, 'MANAGER');

-- Insert regular user for tests
INSERT INTO users (id, username, email, passwordHash, passwordSalt, firstname, lastname, emailVerified) VALUES (3, 'user', 'user@localhost', '$2a$12$arzn9PpRswUrgIw0xaXoM.Y2PeX8m9OKKEGggA07bUXm/O./85WPa', 'Salt', 'Regular', 'User', true);

-- Assign ADMIN tags to regular user
INSERT INTO user_tags (user_id, tags) VALUES (3, 'normaluser');
INSERT INTO user_tags (user_id, tags) VALUES (3, 'demouser');

-- Assign USER role to regular user
INSERT INTO user_roles (user_id, role) VALUES (3, 'USER');

-- Insert supervisor user for tests
INSERT INTO users (id, username, email, passwordHash, passwordSalt, firstname, lastname, emailVerified) VALUES (4, 'supervisor', 'supervisor@localhost', '$2a$12$arzn9PpRswUrgIw0xaXoM.Y2PeX8m9OKKEGggA07bUXm/O./85WPa', 'Salt', 'Supervisor', 'User', true);

-- Assign ADMIN tags to supervisor user
INSERT INTO user_tags (user_id, tags) VALUES (4, 'supervisoruser');
INSERT INTO user_tags (user_id, tags) VALUES (4, 'demouser');

-- Assign USER role to regular user
INSERT INTO user_roles (user_id, role) VALUES (3, 'SUPERVISOR');

-- Insert a minimal event location for tests
INSERT INTO eventlocations (id, name, address, manager_id) VALUES (1, 'Test Hall', 'Teststraße 1, 12345 Teststadt', 2);

-- Insert a minimal event for tests
INSERT INTO events (id, name, startTime, endTime, bookingStartTime, bookingDeadline, reminderSent, event_location_id, manager_id) VALUES (1, 'Test Concert', '2024-12-31 19:00:00', '2024-12-31 21:00:00', '2024-12-12 17:00:00', '2026-12-12 17:00:00', false, 1, 2);

-- Assign supervisor user (id 4) to the event (id 1)
INSERT INTO event_supervisors (event_id, user_id) VALUES (1, 4);

-- Insert areas (referenced by seats via area_id below)
INSERT INTO event_location_areas (id, event_location_id, name) VALUES
(1, 1, 'Ground Floor'),
(2, 1, 'Balcony');

-- Insert seats, split into two areas (Ground Floor on the ground floor, Balcony above)
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate, seatRow, entrance, area_id) VALUES
(1, '1', 1, 1, 1, 'Row 1', 'Entrance A', 1),
(2, '2', 1, 2, 1, 'Row 1', 'Entrance A', 1),
(3, '3', 1, 3, 1, 'Row 1', 'Entrance A', 1),
(4, '4', 1, 4, 1, 'Row 1', 'Entrance A', 1),
(5, '5', 1, 1, 2, 'Balcony 1', 'Entrance B', 2),
(6, '6', 1, 2, 2, 'Balcony 1', 'Entrance B', 2),
(7, '7', 1, 3, 2, 'Balcony 1', 'Entrance B', 2),
(8, '8', 1, 4, 2, 'Balcony 1', 'Entrance B', 2);

-- Update sequence for tables
ALTER SEQUENCE users_seq RESTART WITH 5;
ALTER SEQUENCE eventlocations_seq RESTART WITH 2;
ALTER SEQUENCE events_seq RESTART WITH 2;
ALTER SEQUENCE seats_seq RESTART WITH 9;
ALTER SEQUENCE event_location_areas_seq RESTART WITH 3;