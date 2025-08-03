ALTER TABLE game
ADD COLUMN ended_date timestamp default null,
ADD COLUMN state varchar(10) DEFAULT 'Not start';
-- Default is Not start | Started | Finished | Cancel

ALTER table game
drop constraint game_ibfk_3,
add constraint game_team1_onetoone FOREIGN KEY (team_id1) REFERENCES team(team_id)
 on update cascade on delete cascade,
drop constraint game_ibfk_4,
add constraint game_team2_onetoone FOREIGN KEY (team_id2) REFERENCES team(team_id)
 on update cascade on delete cascade;
 
ALTER TABLE team
ADD COLUMN expense_1 float default 0.0,
ADD COLUMN expense_2 float default 0.0,
ADD COLUMN game_id int not null,
ADD CONSTRAINT team_game_onetoone FOREIGN KEY (game_id) REFERENCES game (game_id);