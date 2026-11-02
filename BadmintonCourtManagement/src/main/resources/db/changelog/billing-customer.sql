--
-- G-9 (P3): buyer directory for repeat/B2B customers. Buyer info stays
-- optional on a POS invoice; this only speeds up checkout.
--
CREATE TABLE IF NOT EXISTS `customer`
(
    `customer_id`  bigint       NOT NULL AUTO_INCREMENT,
    `name`         varchar(120) NOT NULL,
    `company`      varchar(200) DEFAULT NULL,
    `tax_code`     varchar(20)  DEFAULT NULL,
    `address`      varchar(300) DEFAULT NULL,
    `phone`        varchar(30)  DEFAULT NULL,
    `email`        varchar(120) DEFAULT NULL,
    `bank_name`    varchar(120) DEFAULT NULL,
    `bank_account` varchar(40)  DEFAULT NULL,
    `is_active`    tinyint(1)   NOT NULL DEFAULT 1,
    `created_at`   timestamp    NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`customer_id`),
    KEY `idx_customer_name` (`name`),
    KEY `idx_customer_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
