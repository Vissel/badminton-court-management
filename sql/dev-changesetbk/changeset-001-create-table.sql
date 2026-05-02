CREATE TABLE IF NOT EXISTS player (
	player_id int primary key auto_increment,
    player_name varchar(250) ,
    player_password varchar(250) ,
    created_date timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shuttle_ball(
	shuttle_id int primary key auto_increment,
	shuttle_name varchar(250) ,
	cost float,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS court(
	court_id int primary key auto_increment,
	court_name varchar(250) ,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP,
	is_active bit(1)
);

CREATE TABLE IF NOT EXISTS service(
	ser_id int primary key auto_increment,
	ser_name varchar(250) ,
    cost float,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS team(
	team_id int primary key auto_increment,
	player_id1 int null,
	player_id2 int null,
    is_status bit(1) default 0,
    foreign key (player_id1) references player(player_id),
    foreign key (player_id2) references player(player_id)
);    
    
CREATE TABLE IF NOT EXISTS game(
	game_id int primary key auto_increment,
	court_id int not null,
	shuttle_id int not null,
	shuttle_number int default 1,
    team_id1 int,
    team_id2 int,
	created_date timestamp DEFAULT CURRENT_TIMESTAMP,
    foreign key (court_id) references court(court_id),
    foreign key (shuttle_id) references shuttle_ball(shuttle_id),
    foreign key (team_id1) references team(team_id),
    foreign key (team_id2) references team(team_id)
);