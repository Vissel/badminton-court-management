--
-- Soft delete for application users: removing an account from user management
-- keeps its row (audit trail, invoice/debt references) but hides it from every
-- listing and blocks login.
-- NULL means "not removed", so the active filter is `is_deleted IS NULL`.
--
ALTER TABLE `app_user`
    ADD COLUMN `is_deleted` tinyint(1) DEFAULT NULL COMMENT 'soft delete marker, NULL = active';
