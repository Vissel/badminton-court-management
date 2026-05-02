ALTER TABLE shuttle_ball
ADD CONSTRAINT unique_shuttle_name UNIQUE (shuttle_name,is_active);