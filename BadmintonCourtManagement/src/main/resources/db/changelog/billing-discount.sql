--
-- G-10: invoice-level discount (chiết khấu). Stored as a snapshot on the bill
-- and as a negative DISCOUNT line so the charged total stays transparent.
--
ALTER TABLE `invoice`
    ADD COLUMN `discount_amount` decimal(12,2) DEFAULT 0 COMMENT 'total discount applied to this bill';
