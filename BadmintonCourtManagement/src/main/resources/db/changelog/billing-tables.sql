--
-- Billing: immutable, sequentially numbered bill documents issued at payment time.
-- invoice: bill header (buyer/tax/VAT snapshot); invoice_item: snapshot lines.
-- invoice_series: gapless bill numbering, locked inside the payment transaction.
-- bill_config: single-row venue profile + VAT + print/e-invoice settings.
--

CREATE TABLE IF NOT EXISTS `invoice`
(
    `invoice_id`       bigint       NOT NULL AUTO_INCREMENT,
    `bill_no`          varchar(30)  NOT NULL,
    `invoice_type`     varchar(20)  NOT NULL COMMENT 'CHECKOUT | DEBT_SETTLEMENT',
    `session_id`       int          DEFAULT NULL,
    `ava_id`           bigint       DEFAULT NULL,
    `player_id`        int          DEFAULT NULL,
    `payment_id`       bigint       DEFAULT NULL,
    `buyer_name`       varchar(120) DEFAULT NULL,
    `buyer_company`    varchar(200) DEFAULT NULL,
    `buyer_tax_code`   varchar(20)  DEFAULT NULL,
    `buyer_address`    varchar(300) DEFAULT NULL,
    `buyer_email`      varchar(120) DEFAULT NULL,
    `subtotal`         decimal(12,2) NOT NULL COMMENT 'pre-VAT amount extracted from gross charges',
    `vat_rate`         decimal(5,2)  NOT NULL DEFAULT 0 COMMENT 'VAT % snapshot at issue time',
    `vat_amount`       decimal(12,2) NOT NULL DEFAULT 0,
    `total`            decimal(12,2) NOT NULL COMMENT 'gross charged amount (sum of positive lines)',
    `collect_amount`   decimal(12,2) NOT NULL COMMENT 'cash actually collected = total - advance - new debt',
    `currency`         varchar(10)   DEFAULT 'VND',
    `pay_type`         varchar(20)   NOT NULL,
    `status`           varchar(15)   NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED | VOIDED',
    `issued_by`        varchar(50)   NOT NULL,
    `issued_at`        timestamp     NULL DEFAULT CURRENT_TIMESTAMP,
    `voided_by`        varchar(50)   DEFAULT NULL,
    `voided_at`        timestamp     NULL DEFAULT NULL,
    `void_reason`      varchar(250)  DEFAULT NULL,
    `einvoice_status`  varchar(15)   NOT NULL DEFAULT 'NONE' COMMENT 'NONE | PENDING | ISSUED | FAILED | CANCELLED',
    `einvoice_no`      varchar(50)   DEFAULT NULL,
    `einvoice_ref`     varchar(100)  DEFAULT NULL,
    `einvoice_pdf_url` varchar(500)  DEFAULT NULL,
    `einvoice_at`      timestamp     NULL DEFAULT NULL,
    `print_count`      int           NOT NULL DEFAULT 0,
    `last_printed_at`  timestamp     NULL DEFAULT NULL,
    `note`             varchar(250)  DEFAULT NULL,
    `created_date`     timestamp     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`invoice_id`),
    UNIQUE KEY `uq_invoice_bill_no` (`bill_no`),
    KEY `invoice_player_fk` (`player_id`),
    KEY `invoice_session_fk` (`session_id`),
    KEY `invoice_ava_fk` (`ava_id`),
    KEY `invoice_payment_fk` (`payment_id`),
    KEY `idx_invoice_issued_at` (`issued_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `invoice_item`
(
    `item_id`    bigint       NOT NULL AUTO_INCREMENT,
    `invoice_id` bigint       NOT NULL,
    `line_no`    int          NOT NULL,
    `item_type`  varchar(20)  NOT NULL COMMENT 'COURT_FEE | SERVICE | RENT_BY_TIME | DEBT_PAID | DEBT_CREATED | ADVANCE_DEDUCT',
    `item_name`  varchar(200) NOT NULL,
    `qty`        decimal(8,2) DEFAULT 1,
    `unit_price` decimal(12,2) DEFAULT NULL,
    `amount`     decimal(12,2) NOT NULL COMMENT 'line total; negative for deduction lines',
    `note`       varchar(250)  DEFAULT NULL,
    PRIMARY KEY (`item_id`),
    KEY `invoice_item_invoice_fk` (`invoice_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Gapless bill numbering: row locked SELECT ... FOR UPDATE inside the payment tx.
CREATE TABLE IF NOT EXISTS `invoice_series`
(
    `series_key` varchar(20) NOT NULL,
    `current_no` bigint      NOT NULL DEFAULT 0,
    PRIMARY KEY (`series_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `bill_config`
(
    `config_id`          int          NOT NULL AUTO_INCREMENT,
    `business_name`      varchar(200) DEFAULT NULL,
    `tax_code`           varchar(20)  DEFAULT NULL,
    `address`            varchar(300) DEFAULT NULL,
    `phone`              varchar(30)  DEFAULT NULL,
    `bill_prefix`        varchar(10)  NOT NULL DEFAULT 'BL',
    `vat_rate`           decimal(5,2) NOT NULL DEFAULT 0 COMMENT 'default VAT % (price-inclusive)',
    `bill_footer`        varchar(250) DEFAULT NULL,
    `printer_mode`       varchar(15)  NOT NULL DEFAULT 'BROWSER' COMMENT 'BROWSER | NETWORK',
    `printer_ip`         varchar(45)  DEFAULT NULL,
    `printer_port`       int          DEFAULT 9100,
    `paper_width`        int          DEFAULT 80 COMMENT 'receipt paper width mm',
    `einvoice_enabled`   tinyint(1)   NOT NULL DEFAULT 0,
    `einvoice_series`    varchar(20)  DEFAULT NULL,
    `einvoice_template`  varchar(50)  DEFAULT NULL,
    `auto_print`         tinyint(1)   NOT NULL DEFAULT 1 COMMENT 'auto open print after payment',
    `updated_by`         varchar(50)  DEFAULT NULL,
    `updated_at`         timestamp    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`config_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Seed the single config row.
INSERT INTO `bill_config` (`business_name`, `bill_prefix`, `vat_rate`, `printer_mode`, `auto_print`)
SELECT 'Sân cầu lông TC', 'BL', 0, 'BROWSER', 1
WHERE NOT EXISTS (SELECT 1 FROM `bill_config`);

-- Add foreign key constraints after all tables are created
ALTER TABLE `invoice`
    ADD CONSTRAINT `invoice_player_fk` FOREIGN KEY (`player_id`) REFERENCES `player` (`player_id`),
    ADD CONSTRAINT `invoice_session_fk` FOREIGN KEY (`session_id`) REFERENCES `session` (`session_id`),
    ADD CONSTRAINT `invoice_ava_fk` FOREIGN KEY (`ava_id`) REFERENCES `available_player` (`ava_id`),
    ADD CONSTRAINT `invoice_payment_fk` FOREIGN KEY (`payment_id`) REFERENCES `payment` (`payment_id`);

ALTER TABLE `invoice_item`
    ADD CONSTRAINT `invoice_item_invoice_fk` FOREIGN KEY (`invoice_id`) REFERENCES `invoice` (`invoice_id`);
