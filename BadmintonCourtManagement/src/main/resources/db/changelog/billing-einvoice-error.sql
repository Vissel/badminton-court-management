-- Track the last e-invoice submission error on the bill for ops review/retry.
ALTER TABLE `invoice`
    ADD COLUMN `einvoice_error` varchar(500) DEFAULT NULL COMMENT 'last e-invoice submission error, for ops review';
