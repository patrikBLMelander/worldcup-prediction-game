-- Migration: Fix notifications_type_check constraint to include LEAGUE_MEMBER_JOINED
-- Date: 2025-12-19
-- Description: Updates the CHECK constraint on notifications.type to include the new LEAGUE_MEMBER_JOINED enum value
-- 
-- This fixes the constraint violation error when trying to create notifications
-- with type LEAGUE_MEMBER_JOINED (used when a new member joins a league).
--
-- Run this SQL manually in your PostgreSQL database:
-- psql -U worldcup -d worldcupdb -f fix_notifications_type_constraint.sql
-- Or execute via your database management tool

-- Drop the existing constraint
ALTER TABLE notifications 
DROP CONSTRAINT IF EXISTS notifications_type_check;

-- Add the updated constraint with all notification types
ALTER TABLE notifications 
ADD CONSTRAINT notifications_type_check 
CHECK (type IN (
    'ACHIEVEMENT',
    'MATCH_RESULT',
    'LEAGUE_INVITE',
    'LEAGUE_MEMBER_JOINED',
    'LEADERBOARD_POSITION'
));

-- Verify the constraint was created correctly
-- SELECT conname, pg_get_constraintdef(oid) 
-- FROM pg_constraint 
-- WHERE conrelid = 'notifications'::regclass AND contype = 'c';

