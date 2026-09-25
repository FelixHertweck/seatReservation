-- Usernames are compared case-insensitively by the application, so the database
-- has to enforce the same. Existing data must not contain usernames that only
-- differ in upper/lower case; fail with a clear message instead of a cryptic
-- index error so they can be renamed or removed first.
DO $$
DECLARE
    duplicates text;
BEGIN
    SELECT string_agg(name, ', ' ORDER BY name)
    INTO duplicates
    FROM (
        SELECT lower(username) AS name
        FROM users
        GROUP BY lower(username)
        HAVING count(*) > 1
    ) AS d;

    IF duplicates IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot enforce case-insensitive unique usernames. These usernames exist in several spellings: %. Rename or delete the duplicates, then restart.', duplicates;
    END IF;
END $$;

CREATE UNIQUE INDEX users_username_lower_key ON users (lower(username));
