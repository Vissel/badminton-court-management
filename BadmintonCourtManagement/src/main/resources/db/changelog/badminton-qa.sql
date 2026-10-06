-- Base table creation for badminton court management
-- Core tables: player, session, available_player, court, team, shuttle_ball, game, game_shuttle_map, service

CREATE TABLE IF NOT EXISTS `player`
(
    `player_id`    int NOT NULL AUTO_INCREMENT,
    `player_name`  varchar(250) DEFAULT NULL,
    `password`     varchar(250) DEFAULT NULL,
    `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE IF NOT EXISTS `session`
(
    `session_id` int NOT NULL AUTO_INCREMENT,
    `from_time`  timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `to_time`    timestamp NULL DEFAULT NULL,
    `is_active`  bit(1) DEFAULT b'1',
    PRIMARY KEY (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE IF NOT EXISTS `available_player`
(
    `ava_id`     bigint NOT NULL AUTO_INCREMENT,
    `player_id`  int            DEFAULT NULL,
    `session_id` int            DEFAULT NULL,
    `leave_time` timestamp NULL DEFAULT NULL,
    `services`   text,
    `pay_amount` decimal(10, 0) DEFAULT '0',
    `pay_type`   varchar(10)    DEFAULT NULL,
    PRIMARY KEY (`ava_id`),
    KEY          `ava_player_fk1` (`player_id`),
    KEY          `ava_session_fk2` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `court`
(
    `court_id`     int NOT NULL AUTO_INCREMENT,
    `court_name`   varchar(250) DEFAULT NULL,
    `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `is_active`    bit(1)       DEFAULT NULL,
    PRIMARY KEY (`court_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE IF NOT EXISTS `team`
(
    `team_id`    int NOT NULL AUTO_INCREMENT,
    `player_id1` bigint DEFAULT NULL,
    `player_id2` bigint DEFAULT NULL,
    `is_status`  bit(1) DEFAULT b'0',
    `expense_1`  float  DEFAULT '0',
    `expense_2`  float  DEFAULT '0',
    `game_id`    int NOT NULL,
    PRIMARY KEY (`team_id`),
    KEY          `team_game_onetoone` (`game_id`),
    KEY          `team_player1_foreign` (`player_id1`),
    KEY          `team_player2_foreign` (`player_id2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE IF NOT EXISTS `shuttle_ball`
(
    `shuttle_id`   int NOT NULL AUTO_INCREMENT,
    `shuttle_name` varchar(250) DEFAULT NULL,
    `cost`         float        DEFAULT NULL,
    `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `is_active`    bit(1)       DEFAULT NULL,
    `is_selected`  bit(1)       DEFAULT b'0',
    PRIMARY KEY (`shuttle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `game`
(
    `game_id`      int NOT NULL AUTO_INCREMENT,
    `court_id`     int NOT NULL,
    `team_id1`     int         DEFAULT NULL,
    `team_id2`     int         DEFAULT NULL,
    `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `ended_date`   timestamp NULL DEFAULT NULL,
    `state`        varchar(10) DEFAULT 'Not start',
    `gtype`        varchar(10) DEFAULT 'SHARE',
    PRIMARY KEY (`game_id`),
    KEY            `court_id` (`court_id`),
    KEY            `game_team1_onetoone` (`team_id1`),
    KEY            `game_team2_onetoone` (`team_id2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `game_shuttle_map`
(
    `id`             int NOT NULL AUTO_INCREMENT,
    `game_id`        int DEFAULT NULL,
    `shuttle_id`     int DEFAULT NULL,
    `shuttle_number` int DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY              `game_id` (`game_id`),
    KEY              `shuttle_id` (`shuttle_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE IF NOT EXISTS `service`
(
    `ser_id`       int NOT NULL AUTO_INCREMENT,
    `ser_name`     varchar(250) DEFAULT NULL,
    `cost`         float        DEFAULT NULL,
    `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `is_active`    bit(1)       DEFAULT NULL,
    PRIMARY KEY (`ser_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Add foreign key constraints after all tables are created
ALTER TABLE `available_player`
    ADD CONSTRAINT `ava_player_fk1` FOREIGN KEY (`player_id`) REFERENCES `player` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    ADD CONSTRAINT `ava_session_fk2` FOREIGN KEY (`session_id`) REFERENCES `session` (`session_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `team`
    ADD CONSTRAINT `team_game_onetoone` FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`),
    ADD CONSTRAINT `team_player1_foreign` FOREIGN KEY (`player_id1`) REFERENCES `available_player` (`ava_id`),
    ADD CONSTRAINT `team_player2_foreign` FOREIGN KEY (`player_id2`) REFERENCES `available_player` (`ava_id`);

ALTER TABLE `game`
    ADD CONSTRAINT `game_ibfk_1` FOREIGN KEY (`court_id`) REFERENCES `court` (`court_id`),
    ADD CONSTRAINT `game_team1_onetoone` FOREIGN KEY (`team_id1`) REFERENCES `team` (`team_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    ADD CONSTRAINT `game_team2_onetoone` FOREIGN KEY (`team_id2`) REFERENCES `team` (`team_id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `game_shuttle_map`
    ADD CONSTRAINT `game_shuttle_map_ibfk_1` FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`),
    ADD CONSTRAINT `game_shuttle_map_ibfk_2` FOREIGN KEY (`shuttle_id`) REFERENCES `shuttle_ball` (`shuttle_id`);
