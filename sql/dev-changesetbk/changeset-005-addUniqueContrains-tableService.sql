ALTER TABLE service
ADD CONSTRAINT unique_service_name UNIQUE (ser_name,is_active);