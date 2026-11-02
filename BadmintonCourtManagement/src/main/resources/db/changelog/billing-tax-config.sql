--
-- G-8 + G-13 + G-15: per-item VAT rate, bank/pattern config and the invoice
-- kind (GTGT under khấu trừ, bán hàng under trực tiếp/khoán).
--
ALTER TABLE `service`
    ADD COLUMN `vat_rate` decimal(5,2) DEFAULT NULL COMMENT 'VAT % for this catalog row; empty = bill_config default';

ALTER TABLE `invoice_item`
    ADD COLUMN `vat_rate`   decimal(5,2)  DEFAULT NULL COMMENT 'VAT % snapshot for this line',
    ADD COLUMN `vat_amount` decimal(12,2) DEFAULT NULL COMMENT 'VAT contained in amount (price-inclusive)';

ALTER TABLE `bill_config`
    ADD COLUMN `invoice_kind`        varchar(20)  DEFAULT 'BAN_HANG' COMMENT 'GTGT | BAN_HANG',
    ADD COLUMN `einvoice_pattern`    varchar(50)  DEFAULT NULL COMMENT 'ký hiệu mẫu số hoá đơn',
    ADD COLUMN `bank_name`           varchar(120) DEFAULT NULL,
    ADD COLUMN `bank_account`        varchar(40)  DEFAULT NULL,
    ADD COLUMN `einvoice_lookup_base` varchar(300) DEFAULT NULL COMMENT 'buyer lookup portal base for the receipt QR';
