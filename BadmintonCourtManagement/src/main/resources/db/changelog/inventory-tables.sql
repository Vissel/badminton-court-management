--
-- Inventory management: stockable items, wholesale purchase lots, stock movement ledger.
-- Catalog tables (shuttle_ball, service) keep price history; stock attaches to inventory_item
-- so it survives the deactivate+insert price-change convention.
--

CREATE TABLE IF NOT EXISTS `inventory_item`
(
    `item_id`      int          NOT NULL AUTO_INCREMENT,
    `item_name`    varchar(250) NOT NULL,
    `item_type`    varchar(30)  NOT NULL COMMENT 'SHUTTLE_BALL | GOODS',
    `unit`         varchar(50)  DEFAULT NULL,
    `retail_price` decimal(12,2) DEFAULT NULL,
    `is_active`    tinyint(1)   NOT NULL DEFAULT 1,
    `created_date` timestamp    NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`item_id`),
    UNIQUE KEY `uq_inventory_item_name_type` (`item_name`, `item_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `purchase_lot`
(
    `lot_id`        bigint        NOT NULL AUTO_INCREMENT,
    `item_id`       int           NOT NULL,
    `purchase_date` date          NOT NULL,
    `quantity`      int           NOT NULL,
    `unit_cost`     decimal(12,2) NOT NULL,
    `total_cost`    decimal(14,2) NOT NULL,
    `supplier`      varchar(250)  DEFAULT NULL,
    `note`          varchar(250)  DEFAULT NULL,
    `created_date`  timestamp     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`lot_id`),
    KEY `purchase_lot_item_fk` (`item_id`),
    CONSTRAINT `purchase_lot_item_fk` FOREIGN KEY (`item_id`) REFERENCES `inventory_item` (`item_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `stock_movement`
(
    `movement_id`    bigint        NOT NULL AUTO_INCREMENT,
    `item_id`        int           NOT NULL,
    `movement_type`  varchar(30)   NOT NULL COMMENT 'PURCHASE_IN | GAME_CONSUMPTION | RETAIL_SALE | ADJUSTMENT | RETURN',
    `quantity_delta` int           NOT NULL,
    `unit_cost`      decimal(12,2) DEFAULT NULL COMMENT 'purchase unit cost, for weighted avg of remaining stock',
    `ref_type`       varchar(30)   DEFAULT NULL,
    `ref_id`         bigint        DEFAULT NULL,
    `note`           varchar(250)  DEFAULT NULL,
    `created_date`   timestamp     NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`movement_id`),
    KEY `stock_movement_item_fk` (`item_id`),
    KEY `idx_stock_movement_item` (`item_id`, `movement_id`),
    CONSTRAINT `stock_movement_item_fk` FOREIGN KEY (`item_id`) REFERENCES `inventory_item` (`item_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

ALTER TABLE `shuttle_ball`
    ADD COLUMN `item_id` int NULL,
    ADD KEY `shuttle_ball_item_fk` (`item_id`),
    ADD CONSTRAINT `shuttle_ball_item_fk` FOREIGN KEY (`item_id`) REFERENCES `inventory_item` (`item_id`);

ALTER TABLE `service`
    ADD COLUMN `item_id` int NULL,
    ADD KEY `service_item_fk` (`item_id`),
    ADD CONSTRAINT `service_item_fk` FOREIGN KEY (`item_id`) REFERENCES `inventory_item` (`item_id`);

-- Backfill: one inventory item per active catalog name.
INSERT INTO `inventory_item` (`item_name`, `item_type`, `unit`, `retail_price`, `is_active`)
SELECT DISTINCT sb.`shuttle_name`, 'SHUTTLE_BALL', NULL, MAX(sb.`cost`), 1
FROM `shuttle_ball` sb
WHERE sb.`is_active` = 1
  AND NOT EXISTS (SELECT 1 FROM `inventory_item` ii
                  WHERE ii.`item_name` = sb.`shuttle_name` AND ii.`item_type` = 'SHUTTLE_BALL')
GROUP BY sb.`shuttle_name`;

INSERT INTO `inventory_item` (`item_name`, `item_type`, `unit`, `retail_price`, `is_active`)
SELECT DISTINCT s.`ser_name`, 'GOODS', NULL, MAX(s.`cost`), 1
FROM `service` s
WHERE s.`is_active` = 1
  AND s.`ser_name` NOT IN ('costInPerson', 'rentByTime')
  AND NOT EXISTS (SELECT 1 FROM `inventory_item` ii
                  WHERE ii.`item_name` = s.`ser_name` AND ii.`item_type` = 'GOODS')
GROUP BY s.`ser_name`;

-- Link catalog rows to their stock item.
UPDATE `shuttle_ball` sb
JOIN `inventory_item` ii ON ii.`item_name` = sb.`shuttle_name` AND ii.`item_type` = 'SHUTTLE_BALL'
SET sb.`item_id` = ii.`item_id`
WHERE sb.`item_id` IS NULL;

UPDATE `service` s
JOIN `inventory_item` ii ON ii.`item_name` = s.`ser_name` AND ii.`item_type` = 'GOODS'
SET s.`item_id` = ii.`item_id`
WHERE s.`item_id` IS NULL;
