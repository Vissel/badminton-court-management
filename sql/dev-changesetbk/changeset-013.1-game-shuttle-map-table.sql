ALTER TABLE game
add column game_shuttle_id int,
add constraint game_shuttle_map_fk1 foreign key (game_shuttle_id) references game_shuttle_map(id);