-- This file allow to write SQL commands that will be emitted in test and dev.
-- The commands are commented as their support depends of the database

-- Insert admin user
INSERT INTO users (id, username, email, passwordHash, firstname, lastname, emailVerified) VALUES (1, 'admin', 'admin@example.com', '$2a$10$IagMwMnYnQAAq6n2p2oe9OOJJKGB7qp.O7NnVdWD6JFeMHwTSNS4q', 'Admin', 'User', true);

-- Assign ADMIN tags to admin user
INSERT INTO user_tags (user_id, tags) VALUES (1, 'superuser');
INSERT INTO user_tags (user_id, tags) VALUES (1, 'demouser');

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN');

-- Insert manager user
INSERT INTO users (id, username, email, passwordHash, firstname, lastname, emailVerified) VALUES (2, 'manager', 'manager@example.example.com', '$2a$10$S2ge7FYww8TDwL8z.nvjDujgzJMohwInStimGs349vJXRpH8KFzfm', 'Event', 'Manager', true);

-- Assign ADMIN tags to manager user
INSERT INTO user_tags (user_id, tags) VALUES (2, 'manageruser');
INSERT INTO user_tags (user_id, tags) VALUES (2, 'demouser');

-- Assign MANAGER role to manager user
INSERT INTO user_roles (user_id, role) VALUES (2, 'MANAGER');

-- Insert regular user
INSERT INTO users (id, username, email, passwordHash, firstname, lastname, emailVerified) VALUES (3, 'user', 'user@example.com', '$2a$10$x9zL3DyXWpj/.f5WLevXGOVtkvc7JbJ6RwJU.a9VfD1rUgsqLVxMq', 'Regular', 'User', true);

-- Assign ADMIN tags to regular user
INSERT INTO user_tags (user_id, tags) VALUES (3, 'normaluser');
INSERT INTO user_tags (user_id, tags) VALUES (3, 'demouser');

-- Assign USER role to regular user
INSERT INTO user_roles (user_id, role) VALUES (3, 'USER');

-- Insert event_location
INSERT INTO eventlocations (id, name, address, manager_id) VALUES (1, 'Stadthalle', 'Hauptstraße 1, 12345 Musterstadt', 2);

-- Insert event
INSERT INTO events (id, name, startTime, endTime, bookingDeadline, event_location_id, manager_id) VALUES (1, 'Konzert der Stadtkapelle', '2024-12-31 19:00:00', '2024-12-31 21:00:00', '2026-12-12 17:00:00', 1, 2);

-- Insert seats
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate) VALUES (1, '1', 1, 1, 1);
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate) VALUES (2, '2', 1, 2, 2);
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate) VALUES (3, '3', 1, 3, 3);
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate) VALUES (4, '4', 1, 4, 4);
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate) VALUES (5, '5', 1, 5, 5);


-- Update sequence for tables
ALTER SEQUENCE users_seq RESTART WITH 4;
ALTER SEQUENCE eventlocations_seq RESTART WITH 2;
ALTER SEQUENCE events_seq RESTART WITH 2;
ALTER SEQUENCE seats_seq RESTART WITH 6;