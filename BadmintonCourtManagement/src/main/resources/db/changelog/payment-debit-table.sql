--
-- Table structure for table `payment_debit`
-- Many-to-many relationship between payment and debit
-- Tracks how payments are allocated to specific debits
--

CREATE TABLE IF NOT EXISTS `payment_debit`
(
    `payment_debit_id` bigint NOT NULL AUTO_INCREMENT,
    `payment_id`       bigint NOT NULL,
    `debit_id`         int NOT NULL,
    `amount_applied`   decimal(10,2) NOT NULL,
    `created_date`     timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`payment_debit_id`),
    KEY `payment_debit_payment_fk` (`payment_id`),
    KEY `payment_debit_debit_fk` (`debit_id`),
    CONSTRAINT `payment_debit_payment_fk` FOREIGN KEY (`payment_id`) REFERENCES `payment` (`payment_id`),
    CONSTRAINT `payment_debit_debit_fk` FOREIGN KEY (`debit_id`) REFERENCES `debit` (`debit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
