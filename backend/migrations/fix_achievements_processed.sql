-- Fix for achievements_processed column migration
-- Run this SQL manually in your PostgreSQL database if the migration fails

-- Step 1: Add the column as nullable first
ALTER TABLE leagues 
ADD COLUMN IF NOT EXISTS achievements_processed boolean;

-- Step 2: Set default value for existing rows
UPDATE leagues 
SET achievements_processed = false 
WHERE achievements_processed IS NULL;

-- Step 3: Make it NOT NULL with default
ALTER TABLE leagues 
ALTER COLUMN achievements_processed SET NOT NULL,
ALTER COLUMN achievements_processed SET DEFAULT false;

