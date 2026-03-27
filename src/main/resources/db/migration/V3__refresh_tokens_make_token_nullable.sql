-- Fix for legacy schema where `refresh_tokens.token` exists and is NOT NULL,
-- while the application stores a SHA-256 hash in `token_hash`.
-- We only relax NOT NULL constraint so inserts via JPA won't fail.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'refresh_tokens'
          AND column_name = 'token'
    ) THEN
        ALTER TABLE refresh_tokens
            ALTER COLUMN token DROP NOT NULL;
    END IF;
END $$;

