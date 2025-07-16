-- This file allow to write SQL commands that will be emitted in test and dev.
-- The commands are commented as their support depends of the database

-- Insert admin user
INSERT INTO users (id, username, email, passwordHash, firstname, lastname) VALUES (1, 'admin', 'admin@example.com', '$2a$10$IagMwMnYnQAAq6n2p2oe9OOJJKGB7qp.O7NnVdWD6JFeMHwTSNS4q', 'Admin', 'User');

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN');

-- Insert regular user
INSERT INTO users (id, username, email, passwordHash, firstname, lastname) VALUES (2, 'user', 'user@example.com', '$2a$10$x9zL3DyXWpj/.f5WLevXGOVtkvc7JbJ6RwJU.a9VfD1rUgsqLVxMq', 'Regular', 'User');

-- Assign USER role to regular user
INSERT INTO user_roles (user_id, role) VALUES (2, 'USER');

-- Insert manager user
INSERT INTO users (id, username, email, passwordHash, firstname, lastname) VALUES (3, 'manager', 'manager@example.com', '$2a$10$x9zL3DyXWpj/.f5WLevXGOVtkvc7JbJ6RwJU.a9VfD1rUgsqLVxMq', 'Event', 'Manager');

-- Assign MANAGER role to manager user
INSERT INTO user_roles (user_id, role) VALUES (3, 'MANAGER');

-- Update sequence for users table
ALTER SEQUENCE users_seq RESTART WITH 3;

-- Insert event_location
INSERT INTO eventlocations (id, name, address, manager_id) VALUES (1, 'Stadthalle', 'Hauptstraße 1, 12345 Musterstadt', 3);

-- Insert event
INSERT INTO events (id, name, startTime, endTime, bookingDeadline, event_location_id, manager_id) VALUES (1, 'Konzert der Stadtkapelle', '2024-12-31 19:00:00', '2024-12-31 21:00:00', '2024-12-12 17:00:00', 1, 3);

-- Insert seats
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate) VALUES (1, '1', 1, 1, 1);
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate) VALUES (2, '2', 1, 2, 2);
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate) VALUES (3, '3', 1, 3, 3);
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate) VALUES (4, '4', 1, 4, 4);
INSERT INTO seats (id, seatNumber, location_id, xCoordinate, yCoordinate) VALUES (5, '5', 1, 5, 5);