use `bad-court-management-db`;
select * from service;
SELECT * FROM player;

-- session queries
select * from `session`;
SELECT * FROM `session` 
WHERE is_active = true AND to_time is null;

SELECT * FROM `session` ;
SELECT * FROM `session` 
where is_active = true and from_time < current_timestamp();
SELECT * FROM `session` where session_id=10;

insert into session(from_time,is_active)
values (current_time(),true);

-- available players
select a.ava_id, p.player_id, p. player_name, a.services
 from available_player a inner join player p on a.player_id = p.player_id
where session_id = 14 and leave_time is null;

select * from available_player ;

update available_player
set services = 'costInPerson-15000.0'
where ava_id < 21;

-- update 
update `session`
set is_active = false, to_time = CURRENT_TIME()
where session_id = 16;

SELECT * FROM mysql.time_zone_name;
SELECT @@global.time_zone, @@session.time_zone;

SET GLOBAL time_zone = '+07:00';
SET time_zone = '+07:00';

-- 
select * from court;
select * from shuttle_ball;
select * from game;
select * from game_shuttle_map;
select * from team;
select * from service;

insert into court(court_name)
value ('Sân 8');

update court
set court_name = 'Sân 7'
where court_id = 7;

SELECT constraint_name, constraint_type FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
where table_name ='team';
-- query team 1 in game
select g.team_id1 as team1, g.state, g.gtype, t.is_status
from game g right join team t on g.team_id1 = t.team_id
where g.court_id = 5 and g.state = 'Start';
-- query team 2 in game
select g.team_id2 as team2, g.state, g.gtype, t.is_status
from game g right join team t on g.team_id2 = t.team_id
where g.court_id = 5 and g.state = 'Start';

select * from game
where ended_date is null and court_id = 6;

select * from game 
where ended_date is not null
ORDER BY ended_date DESC;

select * from game_shuttle_map;

select * from team 
where team_id in (80,81);
select * from available_player a inner join player p on a.player_id = p.player_id
where a.ava_id in (7, 5);

select * from available_player 
where ava_id in (22,12,14,21,16,5,18,8);

update game
set state = 'Cancel',
ended_date = CURRENT_TIME()
where game_id = 10;

update team
set player_id1 = null
where team_id = 22;
