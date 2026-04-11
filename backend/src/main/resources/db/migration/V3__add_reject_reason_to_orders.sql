-- Add reject_reason column to orders table
ALTER TABLE orders ADD COLUMN reject_reason VARCHAR(500) DEFAULT NULL AFTER payment_status;
