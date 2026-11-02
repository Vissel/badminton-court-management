--
-- G-5: internal-control trail for sensitive bill operations (issue, void,
-- adjust, replace, print, e-invoice, config change).
--
CREATE TABLE IF NOT EXISTS `invoice_log`
(
    `log_id`     bigint       NOT NULL AUTO_INCREMENT,
    `invoice_id` bigint       DEFAULT NULL,
    `bill_no`    varchar(30)  DEFAULT NULL,
    `action`     varchar(30)  NOT NULL COMMENT 'ISSUE | VOID | ADJUST | REPLACE | PRINT | DISCOUNT | EINVOICE_ISSUE | EINVOICE_SEND | CONFIG_UPDATE',
    `actor`      varchar(50)  DEFAULT NULL,
    `ip`         varchar(45)  DEFAULT NULL,
    `detail`     varchar(250) DEFAULT NULL,
    `payload`    varchar(4000) DEFAULT NULL COMMENT 'provider request/response excerpt',
    `error`      varchar(500) DEFAULT NULL,
    `created_at` timestamp    NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`log_id`),
    KEY `idx_invoice_log_invoice` (`invoice_id`),
    KEY `idx_invoice_log_created` (`created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
