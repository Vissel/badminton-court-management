--
-- G-7: payment idempotency. The FE sends one client transaction id per
-- payment attempt; a retry returns the bill already issued for it instead of
-- charging (and declaring) the revenue twice.
--
ALTER TABLE `invoice`
    ADD COLUMN `client_txn_id` varchar(64) DEFAULT NULL COMMENT 'client idempotency key, one per payment attempt';

CREATE UNIQUE INDEX `uq_invoice_client_txn` ON `invoice` (`client_txn_id`);
