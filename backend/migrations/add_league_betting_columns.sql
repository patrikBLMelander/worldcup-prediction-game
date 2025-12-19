-- Migration: Add betting columns to leagues table
-- Date: 2025-12-19
-- Description: Adds columns for the Flat Stakes betting feature
-- 
-- This migration adds the following columns to the leagues table:
-- - betting_type: ENUM('FLAT_STAKES', 'CUSTOM_STAKES')
-- - entry_price: DECIMAL(10,2) - Entry price for Flat Stakes leagues
-- - payout_structure: ENUM('WINNER_TAKES_ALL', 'RANKED')
-- - ranked_percentages: TEXT - JSON map of rank to percentage (e.g., {"1": 0.60, "2": 0.30, "3": 0.10})
--
-- All columns are nullable to allow existing leagues to continue working.
-- Run this SQL manually in your PostgreSQL database:
-- psql -U worldcup -d worldcupdb -f add_league_betting_columns.sql
-- Or execute via your database management tool

-- Add betting_type column (nullable, allows existing leagues to have NULL)
ALTER TABLE leagues 
ADD COLUMN IF NOT EXISTS betting_type VARCHAR(20);

-- Add entry_price column (nullable, allows existing leagues to have NULL)
ALTER TABLE leagues 
ADD COLUMN IF NOT EXISTS entry_price DECIMAL(10,2);

-- Add payout_structure column (nullable, allows existing leagues to have NULL)
ALTER TABLE leagues 
ADD COLUMN IF NOT EXISTS payout_structure VARCHAR(20);

-- Add ranked_percentages column (nullable, stores JSON as TEXT)
ALTER TABLE leagues 
ADD COLUMN IF NOT EXISTS ranked_percentages TEXT;

-- Verify the columns were added correctly
-- SELECT column_name, data_type, is_nullable 
-- FROM information_schema.columns 
-- WHERE table_name = 'leagues' 
-- AND column_name IN ('betting_type', 'entry_price', 'payout_structure', 'ranked_percentages');

