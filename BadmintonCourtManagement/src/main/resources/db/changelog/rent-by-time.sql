--
-- Table structure for table `rent_by_time`
--

CREATE TABLE IF NOT EXISTS `rent_by_time`
(
    `id`         int NOT NULL AUTO_INCREMENT,
    `ava_id`     bigint NOT NULL,
    `court_id`   int NOT NULL,
    `start_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `end_time`   timestamp NULL DEFAULT NULL,
    `num_time`   decimal(4,2) DEFAULT '1.00',
    `shuttles`   varchar(500) DEFAULT NULL,
    `state`      varchar(50) DEFAULT 'Started',
    PRIMARY KEY (`id`),
    KEY `rent_ava_fk` (`ava_id`),
    KEY `rent_court_fk` (`court_id`),
    CONSTRAINT `rent_ava_fk` FOREIGN KEY (`ava_id`) REFERENCES `available_player` (`ava_id`),
    CONSTRAINT `rent_court_fk` FOREIGN KEY (`court_id`) REFERENCES `court` (`court_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
