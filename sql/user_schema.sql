USE `user_schema`;

select * from `user_schema`.user_tbl u inner join `user_schema`.user_auth_tbl a on u.user_id = a.user_id;