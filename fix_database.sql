-- Fix: Remove duplicate 'name' column from policies table
-- You already have 'error_name' column in your entity

ALTER TABLE policies DROP COLUMN IF EXISTS name;
