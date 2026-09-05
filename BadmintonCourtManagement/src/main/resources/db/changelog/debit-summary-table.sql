--
-- Table structure for table `debit_summary`
--

CREATE TABLE IF NOT EXISTS `debit_summary`
(
    `debt_sum_id` bigint NOT NULL AUTO_INCREMENT,
    `total_debts` decimal(10,2) NOT NULL,
    `currency`     varchar(10) DEFAULT 'VND',
    `num_debts`    int NOT NULL,
    `player_id`    int NOT NULL,
    `is_active`    tinyint(1) DEFAULT 1,
    `last_update`  timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`debt_sum_id`),
    KEY `debit_summary_player_fk` (`player_id`),
    CONSTRAINT `debit_summary_player_fk` FOREIGN KEY (`player_id`) REFERENCES `player` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
