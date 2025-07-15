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

-- Update sequence for users table
ALTER SEQUENCE users_seq RESTART WITH 3;