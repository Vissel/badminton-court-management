--
-- Table structure for table `debit`
--

CREATE TABLE IF NOT EXISTS `debit`
(
    `debit_id`         int NOT NULL AUTO_INCREMENT,
    `debt_amount`      decimal(10,2) NOT NULL,
    `remaining_amount` decimal(10,2) NOT NULL,
    `currency`         varchar(10) DEFAULT 'VND',
    `status`           varchar(20) DEFAULT 'PENDING',
    `created_date`     timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `note`             varchar(250) DEFAULT NULL,
    `player_id`        int NOT NULL,
    `session_id`       int NOT NULL,
    PRIMARY KEY (`debit_id`),
    KEY `debit_player_fk` (`player_id`),
    KEY `debit_session_fk` (`session_id`),
    CONSTRAINT `debit_player_fk` FOREIGN KEY (`player_id`) REFERENCES `player` (`player_id`),
    CONSTRAINT `debit_session_fk` FOREIGN KEY (`session_id`) REFERENCES `session` (`session_id`),
    CONSTRAINT `chk_debit_status` CHECK (`status` IN ('PENDING', 'PARTIALLY_PAID', 'PAID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
