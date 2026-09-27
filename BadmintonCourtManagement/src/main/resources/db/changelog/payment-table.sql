--
-- Table structure for table `payment`
--

CREATE TABLE IF NOT EXISTS `payment`
(
    `payment_id`   bigint NOT NULL AUTO_INCREMENT,
    `amount`       decimal(10,2) NOT NULL,
    `currency`     varchar(10) DEFAULT 'VND',
    `payment_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `note`         varchar(250) DEFAULT NULL,
    `player_id`    int NOT NULL,
    PRIMARY KEY (`payment_id`),
    KEY `payment_player_fk` (`player_id`),
    CONSTRAINT `payment_player_fk` FOREIGN KEY (`player_id`) REFERENCES `player` (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
