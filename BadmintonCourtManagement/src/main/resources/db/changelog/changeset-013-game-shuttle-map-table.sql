ALTER TABLE game
DROP CONSTRAINT game_ibfk_2 ;

ALTER TABLE game
drop column shuttle_id;
ALTER TABLE game
drop column shuttle_number;

create table if not exists game_shuttle_map(
	id int primary key auto_increment,
	game_id int,
    shuttle_id int,
    shuttle_number int,
    foreign key (game_id) references game(game_id),
    foreign key (shuttle_id) references shuttle_ball(shuttle_id)
);

ALTER TABLE game
add column game_shuttle_id int,
add constraint game_shuttle_map_fk1 foreign key (game_shuttle_id) references game_shuttle_map(game_shuttle_id);