use `badminton-qa`;
-- 1. Create the user for local access
CREATE USER 'badmintonqa-user01'@'localhost' IDENTIFIED BY 'user01password';

-- 2. Create the user for remote access (%)
CREATE USER 'badmintonqa-user01'@'%' IDENTIFIED BY 'user01password';

-- 3. Grant privileges on the specific schema
GRANT ALL PRIVILEGES ON `badminton-qa`.* TO 'badmintonqa-user01'@'localhost';
GRANT ALL PRIVILEGES ON `badminton-qa`.* TO 'badmintonqa-user01'@'%';

-- 4. Apply the changes
FLUSH PRIVILEGES;

-- verify
SELECT user, host FROM mysql.user WHERE user = 'badmintonqa-user01';