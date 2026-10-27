-- Fee actually charged when a court rental is ended, plus a note explaining a
-- manual override. NULL actual_fee means the rental was billed at its system fee
-- (num_time * hourly rate), so no reconciliation note is needed.
ALTER TABLE `rent_by_time`
    ADD COLUMN `actual_fee` decimal(10,2) DEFAULT NULL COMMENT 'fee actually charged at end of rental; NULL = charged at system fee',
    ADD COLUMN `note`       varchar(250)  DEFAULT NULL COMMENT 'explains difference between actual_fee and the rental system fee';
