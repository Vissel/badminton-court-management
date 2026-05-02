ALTER TABLE team
DROP CONSTRAINT team_ibfk_1 ;
ALTER TABLE team
ADD CONSTRAINT team_player1_foreign FOREIGN KEY (player_id1) REFERENCES available_player(ava_id);

ALTER TABLE team
DROP CONSTRAINT team_ibfk_2 ;
ALTER TABLE team
ADD CONSTRAINT team_player2_foreign FOREIGN KEY (player_id2) REFERENCES available_player(ava_id);