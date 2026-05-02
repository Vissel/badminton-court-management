ALTER TABLE team
MODIFY COLUMN player_id1 bigint,
ADD CONSTRAINT team_player1_foreign FOREIGN KEY (player_id1) REFERENCES available_player(ava_id);

ALTER TABLE team
DROP CONSTRAINT team_ibfk_2,
MODIFY COLUMN player_id2 bigint ;
ALTER TABLE team
ADD CONSTRAINT team_player2_foreign FOREIGN KEY (player_id2) REFERENCES available_player(ava_id);