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

select a.session_id, s.from_time, count(a.ava_id) from available_player a inner join session s on a.session_id = s.session_id
group by (a.session_id)
order by a.session_id desc;

select distinct a. pay_type
 from available_player a ;

-- available players
select a.ava_id, p.player_id, p. player_name, a.services, a. pay_amount, a.is_canceled, a.session_id
 from available_player a inner join player p on a.player_id = p.player_id
where p.player_name ='nguoi choi 05';

select * from player
where player_name ='nguoi choi 03';

select * from available_player
where session_id in (114,108);

select * from session
where session_id in (
	select a.session_id
 from available_player a inner join player p on a.player_id = p.player_id
where p.player_name ='nguoi choi 2' and pay_type is null
);

-- DB changelog
select * from databasechangelog
-- where ID in ('004','005')
where description like '%rent-by-time.sql'
order by ID desc limit 10;

-- debit
 select * from debit_summary
 where player_id = ( select player_id from player where player_name = 'nguoi choi 3');
 
 SELECT COALESCE(SUM(d.debt_amount), 0), COALESCE(SUM(d.remaining_amount), 0), 
            COUNT(d.debit_id), SUM(CASE WHEN d.remaining_amount = 0 THEN 1 ELSE 0 END)
            FROM debit d WHERE d.player_id = 124;

select * from debit where player_id = 124;

 select * from payment
 where player_id = ( select player_id from player where player_name = 'nguoi choi 05');
 
 select * from payment_debit where payment_id in (1,2,3);
 select * from payment
 where player_id = ( select player_id from player where player_name = 'nguoi choi 2');
 
 select * from inventory_item i inner join stock_movement m on i.item_id = m.item_id;