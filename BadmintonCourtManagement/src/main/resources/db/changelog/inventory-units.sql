--
-- Two-level units for stockable items: base unit (e.g. quả) is what the
-- stock_movement ledger always counts in; package_unit (e.g. ống) is the
-- common intake unit converted via units_per_package at purchase time.
--

ALTER TABLE `inventory_item`
    ADD COLUMN `package_unit` varchar(50) DEFAULT NULL COMMENT 'e.g. ống',
    ADD COLUMN `units_per_package` int DEFAULT NULL COMMENT 'base units per package, e.g. 12';

ALTER TABLE `purchase_lot`
    ADD COLUMN `unit` varchar(50) DEFAULT NULL COMMENT 'unit the quantity was bought in (as displayed)',
    ADD COLUMN `base_quantity` int DEFAULT NULL COMMENT 'quantity converted to base units (= ledger delta)';

-- Existing lots were entered in base units before unit support.
UPDATE `purchase_lot` pl
JOIN `inventory_item` ii ON ii.`item_id` = pl.`item_id`
SET pl.`unit` = ii.`unit`,
    pl.`base_quantity` = pl.`quantity`
WHERE pl.`base_quantity` IS NULL;

-- Shuttle ball items: base unit quả, package ống of 12 (editable per item).
UPDATE `inventory_item`
SET `unit` = 'quả',
    `package_unit` = 'ống',
    `units_per_package` = 12
WHERE `item_type` = 'SHUTTLE_BALL'
  AND `units_per_package` IS NULL;
