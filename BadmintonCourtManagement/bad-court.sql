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
select * from available_player 
where session_id not in (1) and leave_time is null;

-- update 
update `session`
set is_active = false, to_time = CURRENT_TIME()
where session_id = 13;

SELECT * FROM mysql.time_zone_name;
SELECT @@global.time_zone, @@session.time_zone;

SET GLOBAL time_zone = '+07:00';
SET time_zone = '+07:00';

-- 
select * from court;
select * from shuttle_ball;
select * from game;
select * from team;

SELECT constraint_name, constraint_type FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
where table_name ='team';

select * from game;

select * from game
where ended_date is null;

select * from team
where team_id in (17,18,22,23,24,25);

update game
set state = 'Cancel',
ended_date = CURRENT_TIME()
where game_id = 10;

update team
set player_id1 = null
where team_id = 22;