--
-- G-3 + G-11: retain the provider XML (accounting archive / 10-year
-- retention) and the buyer retrieval path (link + QR) required by Điều 11
-- NĐ 254/2026.
--
ALTER TABLE `invoice`
    ADD COLUMN `einvoice_xml`        longtext     DEFAULT NULL COMMENT 'original e-invoice XML returned by the provider',
    ADD COLUMN `einvoice_lookup_url` varchar(500) DEFAULT NULL COMMENT 'buyer lookup link encoded in the receipt QR';
