-- Add advance_payment column to available_player table
-- This stores the advance/partial payment collected when a player joins the session
ALTER TABLE available_player
ADD COLUMN advance_payment DECIMAL(10,0) DEFAULT 0;
