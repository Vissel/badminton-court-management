use `bad-court-management-db`;
use `badminton-qa`;
use `badminton-db`;
select * from rent_by_time;
select * from available_player ;
select * from court;
select * from service;
select g.game_id, c.court_id, c.court_name, g. created_date, g.ended_date, g.state, g.gtype , g.team_id1, g.team_id2
 from game g inner join court c on g.court_id = c.court_id 
 order by game_id desc 
 limit 10;
 
 select * from rent_by_time 
 order by id desc;
 
 select * from rent_by_time
 order by id desc;
--   where court_id = 16 and state = 'STARTED';
 select * from `session`
 order by session_id desc
 limit 2;
 describe available_player;
update available_player
set advance_payment = null
where advance_payment = 0;

select * from player;

-- DB changelog
select * from databasechangelog
order by DATEEXECUTED desc limit 10;