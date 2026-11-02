--
-- G-1: correction bills (điều chỉnh / thay thế). An issued e-invoice is never
-- "huỷ" — errors are repaired by a bill that references the original.
--
ALTER TABLE `invoice`
    ADD COLUMN `related_invoice_id` bigint      DEFAULT NULL COMMENT 'original bill this correction replaces/adjusts',
    ADD COLUMN `correction_type`    varchar(20) DEFAULT NULL COMMENT 'DIEU_CHINH | THAY_THE',
    ADD COLUMN `adjust_reason`      varchar(250) DEFAULT NULL COMMENT 'reason stated on the correction bill';

ALTER TABLE `invoice`
    ADD CONSTRAINT `invoice_related_fk` FOREIGN KEY (`related_invoice_id`) REFERENCES `invoice` (`invoice_id`);

CREATE INDEX `idx_invoice_related` ON `invoice` (`related_invoice_id`);
