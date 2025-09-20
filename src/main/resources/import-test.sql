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

-- Update sequence for tables
ALTER SEQUENCE users_seq RESTART WITH 4;