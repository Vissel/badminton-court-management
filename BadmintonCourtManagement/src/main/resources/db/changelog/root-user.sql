-- liquibase formatted sql
-- changeset thach:002
-- comment: Check and insert default user if not exists
INSERT INTO player (player_name,`password`)
SELECT 'rootuser', '$2a$10$l0NHT7MaEB2Y.wKyQMIcRe8CPAOgznd4lx1ZXQmnsyLu2qF.4w.Ti'
FROM (SELECT 1) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM player WHERE player_name = 'root' AND `password` = '$2a$10$l0NHT7MaEB2Y.wKyQMIcRe8CPAOgznd4lx1ZXQmnsyLu2qF.4w.Ti'
) LIMIT 1;