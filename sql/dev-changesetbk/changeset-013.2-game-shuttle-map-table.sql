ALTER TABLE game
drop constraint game_shuttle_map_fk1;
commit;

ALTER TABLE game
drop column game_shuttle_id;
commit;