CREATE TABLE IF NOT EXISTS `session`(
	session_id int primary key auto_increment,
    from_time timestamp DEFAULT CURRENT_TIMESTAMP,
    to_time timestamp null,
    is_active bit(1) default b'1'
);

CREATE TABLE IF NOT EXISTS `available_player`(
	ava_id bigint primary key auto_increment,
    player_id int,
    session_id int,
    CONSTRAINT ava_player_fk1 FOREIGN KEY (player_id) REFERENCES player(player_id) on update cascade on delete cascade,
	CONSTRAINT ava_session_fk2 FOREIGN KEY (session_id) REFERENCES `session`(session_id) on update cascade on delete cascade
);
