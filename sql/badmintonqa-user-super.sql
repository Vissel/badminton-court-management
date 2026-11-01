-- Reference: gain/revoke SUPER for the QA MySQL user.
-- NOTE: MySQL 8.0+ removed the SUPER privilege (split into dynamic
-- privileges), so this block only applies to a MySQL 5.7 server.
-- For MySQL 8.x use the dynamic-privilege equivalents instead.

-- Gain
GRANT SUPER ON *.* TO 'badmintonqa-user'@'localhost';
FLUSH PRIVILEGES;
-- Revoke
REVOKE SUPER ON *.* FROM 'badmintonqa-user'@'localhost';
FLUSH PRIVILEGES;
SELECT VERSION();                                   -- which block to use
SHOW GRANTS FOR 'badmintonqa-user'@'localhost';     -- what the user currently has
