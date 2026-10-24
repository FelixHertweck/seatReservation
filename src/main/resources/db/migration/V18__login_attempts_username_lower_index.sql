-- Login lockout queries filter on lower(username) and attempttime, so index exactly that expression.
CREATE INDEX idx_login_attempts_username_lower_attempttime
    ON login_attempts (lower(username), attempttime)
    WHERE successful = false;
