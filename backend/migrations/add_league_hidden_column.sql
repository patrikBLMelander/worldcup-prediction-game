-- Add hidden column to leagues table for soft delete functionality
-- Hidden leagues are not shown in lists but all data is preserved

ALTER TABLE leagues 
ADD COLUMN IF NOT EXISTS hidden BOOLEAN NOT NULL DEFAULT FALSE;

-- Add comment for documentation
COMMENT ON COLUMN leagues.hidden IS 'Whether this league is hidden (soft delete). Hidden leagues are not shown in lists but data is preserved.';

