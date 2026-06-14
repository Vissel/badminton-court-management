use `bad-court-management-db`;
select * from service where is_active = true;
SELECT * FROM player;
SELECT * FROM court;
select * from available_player 
order by ava_id desc;

select * from `session` order by session_id desc limit  5;
-- delete from `session` where session_id = 83;

INSERT INTO player (player_name,`password`)
SELECT 'rootuser', '$2a$10$l0NHT7MaEB2Y.wKyQMIcRe8CPAOgznd4lx1ZXQmnsyLu2qF.4w.Ti'
FROM (SELECT 1) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM player WHERE player_name = 'rootuser' AND `password` = '$2a$10$l0NHT7MaEB2Y.wKyQMIcRe8CPAOgznd4lx1ZXQmnsyLu2qF.4w.Ti'
) LIMIT 1;

-- session queries
select * from `session`
order by from_time limit 1, 10
;

-- session operation
SELECT * FROM `session` 
WHERE is_active = true AND to_time is null;

SELECT * FROM `session` 
order by from_time desc;

SELECT * FROM `session` 
where is_active = true and from_time < current_timestamp();
SELECT * FROM `session` where session_id = $session_id;

insert into session(from_time,is_active)
values (current_time(),true);

-- update `session`
-- set to_time = null, is_active = b'1'
-- where session_id =69;

-- available players
select a.ava_id, p.player_id, p. player_name, a.services
 from available_player a inner join player p on a.player_id = p.player_id
where session_id = 85;

select * from available_player a inner join player p on a.player_id = p.player_id;


update available_player
set services = 'costInPerson-15000.0';
-- where ava_id < 21;

-- update 
-- update `session`
-- set is_active = false, to_time = CURRENT_TIME()
-- where session_id = 16;

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
select * from available_player;


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

-- See available player expense by name
select t.team_id, t. player_id1 , t.is_status, t.expense_1
from team t inner join available_player ava on t.player_id1 = ava.ava_id
where ava.ava_id = (
	select a.ava_id from available_player a inner join player p on a.player_id = p.player_id where p.player_name ='y');

select t.team_id, t. player_id2 , t.is_status, t.expense_2
from team t inner join available_player ava on t.player_id2 = ava.ava_id
where ava.ava_id = (
	select a.ava_id from available_player a inner join player p on a.player_id = p.player_id where p.player_name ='y');

select * from player where player_name = 'w';

select * from team ;

SELECT
    t.team_id,
    t.player_id1       AS ava_player_id_1,
    p1.player_name     AS player_name_1,
    p1.player_id 		as id1,
    t.player_id2       AS ava_player_id_2,
    p2.player_id		as id2,
    p2.player_name     AS player_name_2,
    t.is_status,
    t.expense_1,
    t.expense_2
FROM team t
JOIN available_player ap1 ON ap1.ava_id = t.player_id1
JOIN player p1            ON p1.player_id = ap1.player_id
JOIN available_player ap2 ON ap2.ava_id = t.player_id2
JOIN player p2            ON p2.player_id = ap2.player_id
WHERE ap1.ava_id in (77,76,91,75,78,94)
or ap2.ava_id in (77,76,91,75,78,94);
-- p1.player_name = 'y'
--   OR p2.player_name = 'y';

select * from player where player_name = 'y';
select * from available_player where player_id = 55;
select * from team where player_id1 = 94 or player_id2 = 94;

-- select for report
select a.session_id, c.court_name, a.ava_id, p.player_name, a.leave_time, a.pay_amount, a.pay_type, a.services
		, t.team_id as TEAM, t.player_id1, t.player_id2
from available_player a inner join player p on a.player_id = p.player_id
	left join team t on a.ava_id = t.player_id1
		left join game g on g.team_id1 = t.team_id
		left join court c on c.court_id = g.court_id
	where a.session_id = 14
;

select a.session_id, c.court_name, a.ava_id, p.player_name, a.leave_time, a.pay_amount, a.pay_type, a.services
		, t.team_id as TEAM, t.player_id1, t.player_id2
from available_player a inner join player p on a.player_id = p.player_id
	left join team t on a.ava_id = t.player_id1
		left join game g on g.team_id2 = t.team_id
		left join court c on c.court_id = g.court_id
	where a.session_id = 14
;

select * from `user` where user_name like 'micro%';

select * from `user` where user_name = 'micro05'
order by created_at desc;

select * from `request` order by created_at desc;

select * from picture order by pic_id desc;
select * from product_picture_map ;

select * from sale_environment order by created_at desc;
select * from product order by product_id desc;
update product set amount = 0;