-- liquibase formatted sql
-- changeset thach:025
CREATE TABLE IF NOT EXISTS `role` (
    `role_id` int NOT NULL AUTO_INCREMENT,
    `role_name` varchar(32) NOT NULL,
    PRIMARY KEY (`role_id`),
    UNIQUE KEY `uk_role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO `role` (`role_name`) VALUES
    ('ROOT'), ('ADMINISTRATOR'), ('COORDINATOR'), ('PLAYER');

CREATE TABLE IF NOT EXISTS `app_user` (
    `user_id` bigint NOT NULL AUTO_INCREMENT,
    `username` varchar(250) NOT NULL,
    `password` varchar(250) NOT NULL,
    `display_name` varchar(250) DEFAULT NULL,
    `is_active` tinyint(1) NOT NULL DEFAULT 1,
    `created_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_app_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `app_user_role` (
    `user_id` bigint NOT NULL,
    `role_id` int NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `app_user` (`username`, `password`, `display_name`, `is_active`, `created_date`)
SELECT p.player_name, MAX(p.password), p.player_name, 1, MIN(COALESCE(p.created_date, CURRENT_TIMESTAMP))
FROM player p
WHERE p.password IS NOT NULL AND p.password <> ''
GROUP BY p.player_name
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    is_active = 1;

INSERT IGNORE INTO `app_user_role` (`user_id`, `role_id`)
SELECT u.user_id, r.role_id
FROM app_user u
JOIN role r ON r.role_name = CASE WHEN u.username = 'rootuser' THEN 'ROOT' ELSE 'ADMINISTRATOR' END;

-- changeset thach:026
CREATE TEMPORARY TABLE player_canonical AS
SELECT player_name, MIN(player_id) AS canonical_id
FROM player
GROUP BY player_name;

UPDATE available_player target
JOIN player duplicate_player ON duplicate_player.player_id = target.player_id
JOIN player_canonical canonical ON canonical.player_name = duplicate_player.player_name
SET target.player_id = canonical.canonical_id
WHERE target.player_id <> canonical.canonical_id;

UPDATE debit target
JOIN player duplicate_player ON duplicate_player.player_id = target.player_id
JOIN player_canonical canonical ON canonical.player_name = duplicate_player.player_name
SET target.player_id = canonical.canonical_id
WHERE target.player_id <> canonical.canonical_id;

UPDATE debit_summary target
JOIN player duplicate_player ON duplicate_player.player_id = target.player_id
JOIN player_canonical canonical ON canonical.player_name = duplicate_player.player_name
SET target.player_id = canonical.canonical_id
WHERE target.player_id <> canonical.canonical_id;

UPDATE payment target
JOIN player duplicate_player ON duplicate_player.player_id = target.player_id
JOIN player_canonical canonical ON canonical.player_name = duplicate_player.player_name
SET target.player_id = canonical.canonical_id
WHERE target.player_id <> canonical.canonical_id;

DELETE duplicate_player
FROM player duplicate_player
JOIN player_canonical canonical ON canonical.player_name = duplicate_player.player_name
WHERE duplicate_player.player_id <> canonical.canonical_id;

DROP TEMPORARY TABLE player_canonical;
ALTER TABLE player ADD CONSTRAINT uk_player_name UNIQUE (player_name);
ALTER TABLE player DROP COLUMN password;

-- changeset thach:027
CREATE TABLE IF NOT EXISTS `refresh_token` (
    `refresh_token_id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `token_hash` char(64) NOT NULL,
    `expires_at` timestamp(6) NOT NULL,
    `revoked` tinyint(1) NOT NULL DEFAULT 0,
    `created_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`refresh_token_id`),
    UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
    KEY `idx_refresh_token_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Add foreign key constraints after all tables are created
ALTER TABLE `app_user_role`
    ADD CONSTRAINT `app_user_role_user_fk` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    ADD CONSTRAINT `app_user_role_role_fk` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`);

ALTER TABLE `refresh_token`
    ADD CONSTRAINT `refresh_token_user_fk` FOREIGN KEY (`user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE;
