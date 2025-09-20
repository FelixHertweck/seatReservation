-- Auto-cleanup mechanism for email_verification table
-- This script creates a trigger that automatically deletes expired email verification entries

-- Function to cleanup expired email verification entries
CREATE OR REPLACE FUNCTION cleanup_expired_email_verifications()
RETURNS TRIGGER AS $$
BEGIN
    -- Delete expired entries when a new one is inserted or when an entry is updated
    -- This approach distributes the cleanup load across normal operations
    DELETE FROM email_verification 
    WHERE expiration_time < NOW() 
    AND id NOT IN (
        -- Keep the most recent 100 expired entries to avoid race conditions
        SELECT id FROM email_verification 
        WHERE expiration_time < NOW() 
        ORDER BY expiration_time DESC 
        LIMIT 100
    );
    
    -- If this is an INSERT trigger, return NEW, otherwise return NULL
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger that fires after INSERT to cleanup expired entries
-- This distributes cleanup work across normal application usage
CREATE OR REPLACE TRIGGER trigger_cleanup_expired_email_verifications
    AFTER INSERT ON email_verification
    FOR EACH ROW
    EXECUTE FUNCTION cleanup_expired_email_verifications();

-- Create an index for efficient expiration_time lookups
CREATE INDEX IF NOT EXISTS idx_email_verification_expired 
ON email_verification (expiration_time);

-- Optional: Create a more aggressive cleanup function that can be called manually or via cron
CREATE OR REPLACE FUNCTION manual_cleanup_expired_email_verifications()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM email_verification 
    WHERE expiration_time < NOW();
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    -- Log the cleanup operation
    RAISE NOTICE 'Cleaned up % expired email verification entries', deleted_count;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Optional: Set up pg_cron job if the extension is available
-- This would require: CREATE EXTENSION IF NOT EXISTS pg_cron;
-- SELECT cron.schedule('email-verification-cleanup', '0 */6 * * *', 'SELECT manual_cleanup_expired_email_verifications();');