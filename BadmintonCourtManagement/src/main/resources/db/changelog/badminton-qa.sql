-- MySQL dump 10.13  Distrib 8.0.42, for macos15 (x86_64)
--
-- Host: 127.0.0.1    Database: bad-court-management-db
-- ------------------------------------------------------
-- Server version	8.4.5

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `player`
--

DROP TABLE IF EXISTS `player`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `player`
(
    `player_id`    int NOT NULL AUTO_INCREMENT,
    `player_name`  varchar(250) DEFAULT NULL,
    `password`     varchar(250) DEFAULT NULL,
    `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`player_id`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `session`
--

DROP TABLE IF EXISTS `session`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `session`
(
    `session_id` int NOT NULL AUTO_INCREMENT,
    `from_time`  timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `to_time`    timestamp NULL DEFAULT NULL,
    `is_active`  bit(1) DEFAULT b'1',
    PRIMARY KEY (`session_id`)
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `available_player`
--

DROP TABLE IF EXISTS `available_player`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `available_player`
(
    `ava_id`     bigint NOT NULL AUTO_INCREMENT,
    `player_id`  int            DEFAULT NULL,
    `session_id` int            DEFAULT NULL,
    `leave_time` timestamp NULL DEFAULT NULL,
    `services`   text,
    `pay_amount` decimal(10, 0) DEFAULT '0',
    `pay_type`   varchar(10)    DEFAULT NULL,
    PRIMARY KEY (`ava_id`),
    KEY          `ava_player_fk1` (`player_id`),
    KEY          `ava_session_fk2` (`session_id`),
    CONSTRAINT `ava_player_fk1` FOREIGN KEY (`player_id`) REFERENCES `player` (`player_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `ava_session_fk2` FOREIGN KEY (`session_id`) REFERENCES `session` (`session_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `court`
--

DROP TABLE IF EXISTS `court`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `court`
(
    `court_id`     int NOT NULL AUTO_INCREMENT,
    `court_name`   varchar(250) DEFAULT NULL,
    `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `is_active`    bit(1)       DEFAULT NULL,
    PRIMARY KEY (`court_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `team`
--

DROP TABLE IF EXISTS `team`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `team`
(
    `team_id`    int NOT NULL AUTO_INCREMENT,
    `player_id1` bigint DEFAULT NULL,
    `player_id2` bigint DEFAULT NULL,
    `is_status`  bit(1) DEFAULT b'0',
    `expense_1`  float  DEFAULT '0',
    `expense_2`  float  DEFAULT '0',
    `game_id`    int NOT NULL,
    PRIMARY KEY (`team_id`),
    KEY          `team_game_onetoone` (`game_id`),
    KEY          `team_player1_foreign` (`player_id1`),
    KEY          `team_player2_foreign` (`player_id2`),
    CONSTRAINT `team_game_onetoone` FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`),
    CONSTRAINT `team_player1_foreign` FOREIGN KEY (`player_id1`) REFERENCES `available_player` (`ava_id`),
    CONSTRAINT `team_player2_foreign` FOREIGN KEY (`player_id2`) REFERENCES `available_player` (`ava_id`)
) ENGINE=InnoDB AUTO_INCREMENT=183 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


--
-- Table structure for table `shuttle_ball`
--

DROP TABLE IF EXISTS `shuttle_ball`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shuttle_ball`
(
    `shuttle_id`   int NOT NULL AUTO_INCREMENT,
    `shuttle_name` varchar(250) DEFAULT NULL,
    `cost`         float        DEFAULT NULL,
    `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `is_active`    bit(1)       DEFAULT NULL,
    `is_selected`  bit(1)       DEFAULT b'0',
    PRIMARY KEY (`shuttle_id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `game`
--

DROP TABLE IF EXISTS `game`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game`
(
    `game_id`      int NOT NULL AUTO_INCREMENT,
    `court_id`     int NOT NULL,
    `team_id1`     int         DEFAULT NULL,
    `team_id2`     int         DEFAULT NULL,
    `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `ended_date`   timestamp NULL DEFAULT NULL,
    `state`        varchar(10) DEFAULT 'Not start',
    `gtype`        varchar(10) DEFAULT 'SHARE',
    PRIMARY KEY (`game_id`),
    KEY            `court_id` (`court_id`),
    KEY            `game_team1_onetoone` (`team_id1`),
    KEY            `game_team2_onetoone` (`team_id2`),
    CONSTRAINT `game_ibfk_1` FOREIGN KEY (`court_id`) REFERENCES `court` (`court_id`),
    CONSTRAINT `game_team1_onetoone` FOREIGN KEY (`team_id1`) REFERENCES `team` (`team_id`) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `game_team2_onetoone` FOREIGN KEY (`team_id2`) REFERENCES `team` (`team_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=93 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `game_shuttle_map`
--

DROP TABLE IF EXISTS `game_shuttle_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `game_shuttle_map`
(
    `id`             int NOT NULL AUTO_INCREMENT,
    `game_id`        int DEFAULT NULL,
    `shuttle_id`     int DEFAULT NULL,
    `shuttle_number` int DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY              `game_id` (`game_id`),
    KEY              `shuttle_id` (`shuttle_id`),
    CONSTRAINT `game_shuttle_map_ibfk_1` FOREIGN KEY (`game_id`) REFERENCES `game` (`game_id`),
    CONSTRAINT `game_shuttle_map_ibfk_2` FOREIGN KEY (`shuttle_id`) REFERENCES `shuttle_ball` (`shuttle_id`)
) ENGINE=InnoDB AUTO_INCREMENT=88 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `service`
--

DROP TABLE IF EXISTS `service`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `service`
(
    `ser_id`       int NOT NULL AUTO_INCREMENT,
    `ser_name`     varchar(250) DEFAULT NULL,
    `cost`         float        DEFAULT NULL,
    `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `is_active`    bit(1)       DEFAULT NULL,
    PRIMARY KEY (`ser_id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-15 10:08:33
