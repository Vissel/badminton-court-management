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

select * from available_player a inner join session s on a.session_id = s.session_id
where a.player_id = (
select player_id from player
where player_name ='nguoi choi 3');

select * from player
where player_name ='nguoi choi 3';

select * from available_player
where session_id = 129;

-- DB changelog
select * from databasechangelog
-- where ID in ('004','005')
where description like '%rent-by-time.sql'
order by ID desc limit 10;

-- debit
 select * from debit_summary
 where player_id = ( select player_id from player where player_name = 'nguoi choi 3');
 
 select * from payment
 where player_id = ( select player_id from player where player_name = 'nguoi choi 2');
 
 select * from payment_debit where payment_id in (1,2,3);
 select payment_id from payment
 where player_id = ( select player_id from player where player_name = 'nguoi choi 2');