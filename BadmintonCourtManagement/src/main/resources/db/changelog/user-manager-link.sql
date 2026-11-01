-- Manager-subordinate mapping (1 user manages n users): an administrator is
-- linked to the coordinators they manage. Used to scope GET /api/v1/users so
-- an administrator only sees their own coordinators; root sees everyone.
-- Also reserves an external application identifier on app_user for future use.
ALTER TABLE `app_user`
    ADD COLUMN `app_id` varchar(50) DEFAULT NULL COMMENT 'external application identifier, reserved for future use';

CREATE TABLE IF NOT EXISTS `app_user_manager_link` (
    `link_id`          bigint NOT NULL AUTO_INCREMENT,
    `manager_user_id`  bigint NOT NULL COMMENT 'managing user (administrator)',
    `managed_user_id`  bigint NOT NULL COMMENT 'subordinate user (coordinator)',
    `created_date`     timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`link_id`),
    UNIQUE KEY `uk_link_manager_managed` (`manager_user_id`, `managed_user_id`),
    UNIQUE KEY `uk_link_managed_user` (`managed_user_id`),
    KEY `idx_link_manager_user` (`manager_user_id`),
    CONSTRAINT `fk_link_manager_user` FOREIGN KEY (`manager_user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_link_managed_user` FOREIGN KEY (`managed_user_id`) REFERENCES `app_user` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
