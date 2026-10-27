-- Heal catalog rows that lost (or never had) their inventory item link.
-- Services and shuttle balls created from Setup / product import are inserted
-- without item_id, which silently disables stock tracking: the picker gets no
-- itemId (so the client never calls /api/inventory/checkStock) and adding the
-- line to a player writes no RETAIL_SALE movement.
-- Idempotent: only rows with item_id NULL are touched.

UPDATE `service` s
JOIN `inventory_item` ii
    ON ii.`item_name` = s.`ser_name` AND ii.`item_type` = 'GOODS'
SET s.`item_id` = ii.`item_id`
WHERE s.`item_id` IS NULL;

UPDATE `shuttle_ball` sb
JOIN `inventory_item` ii
    ON ii.`item_name` = sb.`shuttle_name` AND ii.`item_type` = 'SHUTTLE_BALL'
SET sb.`item_id` = ii.`item_id`
WHERE sb.`item_id` IS NULL;
