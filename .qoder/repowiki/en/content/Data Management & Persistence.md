# Data Management & Persistence

<cite>
**Referenced Files in This Document**
- [Session.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Session.java)
- [Game.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java)
- [Player.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Player.java)
- [Team.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Team.java)
- [Court.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Court.java)
- [ShuttleBall.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/ShuttleBall.java)
- [AvailablePlayer.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java)
- [GameShuttleMap.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/GameShuttleMap.java)
- [RentByTime.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java)
- [SessionRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java)
- [GameRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/GameRepository.java)
- [AvailablePlayerRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java)
- [TeamRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/TeamRepository.java)
- [UserRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/UserRepository.java)
- [RentByTimeRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/RentByTimeRepository.java)
- [SessionParam.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/filter/SessionParam.java)
- [ExcelExportService.java](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java)
- [RentByTimeService.java](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java)
- [RentByTimeRequest.java](file://BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/RentByTimeRequest.java)
- [RentByTimeResponse.java](file://BadmintonCourtManagement/src/main/java/com/badminton/response/RentByTimeResponse.java)
- [RentShuttleDTO.java](file://BadmintonCourtManagement/src/main/java/com/badminton/model/dto/RentShuttleDTO.java)
- [RentConstant.java](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/RentConstant.java)
- [CourtManagementController.java](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java)
- [ReportCost.java](file://BadmintonCourtManagement/src/main/java/com/badminton/model/dto/ReportCost.java)
- [TeamDTO.java](file://BadmintonCourtManagement/src/main/java/com/badminton/model/dto/TeamDTO.java)
- [TeamResult.java](file://BadmintonCourtManagement/src/main/java/com/badminton/response/result/TeamResult.java)
- [RptModel.java](file://BadmintonCourtManagement/src/main/java/com/badminton/model/report/RptModel.java)
- [db.changelog-master.xml](file://BadmintonCourtManagement/src/main/resources/db/changelog/db.changelog-master.xml)
- [badminton-qa.sql](file://BadmintonCourtManagement/src/main/resources/db/changelog/badminton-qa.sql)
- [court-creation.sql](file://BadmintonCourtManagement/src/main/resources/db/changelog/court-creation.sql)
- [rent-by-time.sql](file://BadmintonCourtManagement/src/main/resources/db/changelog/rent-by-time.sql)
- [application.properties](file://BadmintonCourtManagement/src/main/resources/application.properties)
- [pom.xml](file://BadmintonCourtManagement/pom.xml)
</cite>

## Update Summary
**Changes Made**
- Added comprehensive RentByTime database schema with new rent_by_time table and foreign key relationships
- Integrated RentByTime entity with JPA annotations, cascade operations, and business logic
- Implemented RentByTimeRepository with specialized query methods for court, player, and state-based lookups
- Added RentByTimeService with full CRUD operations, state management, and fee calculation
- Extended Liquibase migration strategy with new rent-by-time.sql changelog
- Introduced RentByTimeRequest and RentByTimeResponse models for API integration
- Added RentShuttleDTO for shuttle ball management in rental operations
- Enhanced controller endpoints for rent-by-time operations with apply, pay, cancel, and update functionality

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Enhanced Querying Capabilities](#enhanced-querying-capabilities)
7. [Advanced Reporting and Analytics](#advanced-reporting-and-analytics)
8. [User Management Patterns](#user-management-patterns)
9. [Rent-by-Time System Integration](#rent-by-time-system-integration)
10. [Database Schema Evolution](#database-schema-evolution)
11. [Dependency Analysis](#dependency-analysis)
12. [Performance Considerations](#performance-considerations)
13. [Troubleshooting Guide](#troubleshooting-guide)
14. [Conclusion](#conclusion)
15. [Appendices](#appendices)

## Introduction
This document explains the data management and persistence layer of the Badminton Court Management system. It covers the database schema design, JPA entity relationships, repository patterns, and Liquibase migration strategy. The system now features enhanced capabilities for player expense tracking, team composition analysis, user management patterns, and comprehensive rent-by-time operations, providing complete support for hourly court rentals with shuttle ball management and real-time state tracking.

## Project Structure
The persistence layer is organized around:
- Entities under the entity package representing domain objects and their JPA mappings
- Repositories under the repository package implementing Spring Data JPA interfaces
- Liquibase changelogs under resources/db/changelog for schema initialization and evolution
- Service layer with advanced business logic including rent-by-time operations
- Request/response models for API integration
- Application configuration for JPA/Hibernate and Liquibase behavior

```mermaid
graph TB
subgraph "Enhanced Entities"
E_Session["Session.java"]
E_Game["Game.java"]
E_Player["Player.java"]
E_Team["Team.java (Enhanced)"]
E_Court["Court.java"]
E_Shuttle["ShuttleBall.java"]
E_Avail["AvailablePlayer.java"]
E_Map["GameShuttleMap.java"]
E_Rent["RentByTime.java (NEW)"]
end
subgraph "Enhanced Repositories"
R_Session["SessionRepository.java"]
R_Game["GameRepository.java"]
R_Avail["AvailablePlayerRepository.java"]
R_Team["TeamRepository.java"]
R_User["UserRepository.java"]
R_Rent["RentByTimeRepository.java (NEW)"]
end
subgraph "Business Services"
S_Export["ExcelExportService.java"]
S_Rent["RentByTimeService.java (NEW)"]
end
subgraph "API Models"
M_Request["RentByTimeRequest.java (NEW)"]
M_Response["RentByTimeResponse.java (NEW)"]
M_DTO["RentShuttleDTO.java (NEW)"]
end
subgraph "Liquibase"
L_Master["db.changelog-master.xml"]
L_QA["badminton-qa.sql"]
L_Courts["court-creation.sql"]
L_Rent["rent-by-time.sql (NEW)"]
end
subgraph "Configuration"
C_AppProps["application.properties"]
C_Pom["pom.xml"]
end
E_Session --> E_Avail
E_Player --> E_Avail
E_Court --> E_Game
E_Court --> E_Rent
E_Game --> E_Team
E_Game --> E_Map
E_Shuttle --> E_Map
E_Avail --> E_Rent
R_Session --> E_Session
R_Game --> E_Game
R_Avail --> E_Avail
R_Team --> E_Team
R_User --> E_Player
R_Rent --> E_Rent
S_Export --> R_Avail
S_Export --> R_Game
S_Rent --> R_Rent
S_Rent --> R_Avail
S_Rent --> R_Court
L_Master --> L_QA
L_Master --> L_Courts
L_Master --> L_Rent
C_AppProps --> L_Master
C_Pom --> L_Master
```

**Diagram sources**
- [Session.java:17-41](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Session.java#L17-L41)
- [Game.java:13-79](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L13-L79)
- [Player.java:15-39](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Player.java#L15-L39)
- [Team.java:8-40](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Team.java#L8-L40)
- [Court.java:11-43](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Court.java#L11-L43)
- [ShuttleBall.java:11-62](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/ShuttleBall.java#L11-L62)
- [AvailablePlayer.java:12-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L12-L58)
- [GameShuttleMap.java:7-31](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/GameShuttleMap.java#L7-L31)
- [RentByTime.java:16-56](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java#L16-L56)
- [RentByTimeRepository.java:10-20](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/RentByTimeRepository.java#L10-L20)
- [RentByTimeService.java:35-255](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L35-L255)
- [RentByTimeRequest.java:11-21](file://BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/RentByTimeRequest.java#L11-L21)
- [RentByTimeResponse.java:12-24](file://BadmintonCourtManagement/src/main/java/com/badminton/response/RentByTimeResponse.java#L12-L24)
- [RentShuttleDTO.java:12-18](file://BadmintonCourtManagement/src/main/java/com/badminton/model/dto/RentShuttleDTO.java#L12-L18)

**Section sources**
- [application.properties:1-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L1-L19)
- [pom.xml:37-112](file://BadmintonCourtManagement/pom.xml#L37-L112)

## Core Components
This section documents the core entities and their relationships, highlighting JPA annotations, foreign keys, and cascade behaviors with enhanced expense tracking capabilities and the new RentByTime system.

- Session
  - Identity column mapped via generated value
  - Timestamp fields for session window
  - Bidirectional relationship with AvailablePlayer using cascade-all
  - Default active flag set on construction

- Game
  - Identity column mapped via generated value
  - Many-to-one to Court
  - One-to-one to Team for both teams with cascade-all
  - Collection of GameShuttleMap with cascade-all and orphan removal
  - State and type fields with defaults

- Team (Enhanced)
  - Identity column mapped via generated value
  - Two AvailablePlayer relationships for player composition
  - Dedicated expense tracking: expenseOne and expenseTwo columns
  - Win status indicator (is_status) for match results
  - One-to-one relationship with Game (mapped-by)

- Court
  - Identity column mapped via generated value
  - Active flag and creation timestamp
  - Collection of Games mapped by
  - Collection of RentByTime entries for hourly rentals

- ShuttleBall
  - Identity column mapped via generated value
  - Name, cost, activity flags, selection flag
  - Creation timestamp

- AvailablePlayer
  - Identity column mapped via generated value
  - Many-to-one to Player and Session
  - Optional leave time, services, payment amount, and type
  - Enhanced with expense tracking integration
  - Collection of RentByTime entries for hourly rentals

- GameShuttleMap
  - Identity column mapped via generated value
  - Many-to-one to Game
  - One-to-one to ShuttleBall with cascade-detach
  - Quantity field

- Player (New)
  - Identity column mapped via generated value
  - Username and password fields for authentication
  - Timestamp for account creation
  - Supports user management and access control

- RentByTime (New)
  - Identity column mapped via generated value
  - Many-to-one relationships to AvailablePlayer and Court
  - Start and end time timestamps for rental period
  - Number of hours with precision and scale for billing
  - Shuttle balls JSON string for equipment tracking
  - State field for rental lifecycle management
  - Constructor with all business-relevant fields

**Section sources**
- [Session.java:17-41](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Session.java#L17-L41)
- [Game.java:13-79](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L13-L79)
- [Team.java:8-40](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Team.java#L8-L40)
- [Court.java:11-43](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Court.java#L11-L43)
- [ShuttleBall.java:11-62](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/ShuttleBall.java#L11-L62)
- [AvailablePlayer.java:12-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L12-L58)
- [GameShuttleMap.java:7-31](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/GameShuttleMap.java#L7-L31)
- [Player.java:15-39](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Player.java#L15-L39)
- [RentByTime.java:16-56](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java#L16-L56)

## Architecture Overview
The persistence architecture follows a layered design with enhanced capabilities including comprehensive rent-by-time operations:
- Entities encapsulate domain data, relationships, expense tracking, and rental lifecycle management
- Repositories define data access contracts with specialized query methods for rentals and availability
- Business service layer provides complete rent-by-time operations with state management and fee calculation
- Advanced service layer provides reporting and analytics capabilities
- Liquibase manages schema initialization and evolution including new rental table
- Spring Data JPA and Hibernate handle persistence operations with proper transaction management

```mermaid
classDiagram
class Session {
+int sessionId
+Instant fromTime
+Instant toTime
+boolean isActive
}
class AvailablePlayer {
+long avaId
+Player player
+Session session
+Instant leaveTime
+String services
+Float payAmount
+String payType
}
class Player {
+int playerId
+String playerName
+String password
+Timestamp createdDate
}
class Court {
+int courtId
+String courtName
+boolean isActive
}
class Game {
+int gameId
+Court court
+Team teamOne
+Team teamTwo
+Instant createdDate
+Instant endedDate
+String state
+String gtype
}
class Team {
+int teamId
+AvailablePlayer playerOne
+float expenseOne
+AvailablePlayer playerTwo
+float expenseTwo
+boolean win
}
class ShuttleBall {
+int shuttleId
+String shuttleName
+float cost
+boolean isActive
+boolean isSelected
}
class GameShuttleMap {
+int id
+Game game
+ShuttleBall shuttleBall
+int shuttleNumber
}
class RentByTime {
+int id
+AvailablePlayer availablePlayer
+Court court
+Instant startTime
+Instant endTime
+BigDecimal numTime
+String shuttles
+String state
}
class TeamRepository {
<<interface>>
}
class UserRepository {
<<interface>>
}
class RentByTimeRepository {
<<interface>>
+findByCourtCourtIdAndState()
+findByAvailablePlayerAvaId()
+findByCourtCourtId()
+findByState()
}
class RentByTimeService {
<<service>>
+applyRentByTime()
+payRentByTime()
+cancelRentByTime()
+updateRentByTime()
+getActiveRentByTimeForCourt()
}
Session "1" --> "*" AvailablePlayer : "mappedBy"
Player "1" --> "*" AvailablePlayer : "mappedBy"
Court "1" --> "*" Game : "mappedBy"
Court "1" --> "*" RentByTime : "mappedBy"
Game "1" --> "1..2" Team : "teamOne/teamTwo"
Game "1" --> "*" GameShuttleMap : "mappedBy"
ShuttleBall "1" --> "*" GameShuttleMap : "mappedBy"
AvailablePlayer "1" --> "*" RentByTime : "mappedBy"
TeamRepository "1" --> "Team" : "CRUD"
UserRepository "1" --> "Player" : "CRUD"
RentByTimeRepository "1" --> "RentByTime" : "CRUD"
RentByTimeService "1" --> "RentByTimeRepository" : "uses"
RentByTimeService "1" --> "CourtRepository" : "uses"
RentByTimeService "1" --> "AvailablePlayerRepository" : "uses"
```

**Diagram sources**
- [Session.java:17-41](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Session.java#L17-L41)
- [AvailablePlayer.java:12-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L12-L58)
- [Player.java:15-39](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Player.java#L15-L39)
- [Court.java:11-43](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Court.java#L11-L43)
- [Game.java:13-79](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L13-L79)
- [Team.java:8-40](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Team.java#L8-L40)
- [ShuttleBall.java:11-62](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/ShuttleBall.java#L11-L62)
- [GameShuttleMap.java:7-31](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/GameShuttleMap.java#L7-L31)
- [RentByTime.java:16-56](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java#L16-L56)
- [TeamRepository.java:1-10](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/TeamRepository.java#L1-L10)
- [UserRepository.java:1-16](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/UserRepository.java#L1-L16)
- [RentByTimeRepository.java:10-20](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/RentByTimeRepository.java#L10-L20)

## Detailed Component Analysis

### Database Schema Design and Liquibase Migration Strategy
- Master changelog orchestrates initial schema and seed data
  - Includes QA schema SQL, root user creation, and default courts
  - **Updated** Now includes rent-by-time.sql for new rental table
- QA schema script defines tables and constraints:
  - player, session, available_player, court, team, shuttle_ball, game, game_shuttle_map, service
  - Foreign keys enforce referential integrity across entities
  - Indexes on frequently filtered/joined columns
- Rent-by-time migration script creates comprehensive rental table:
  - Primary key id with auto-increment
  - Foreign keys to available_player (ava_id) and court (court_id)
  - Timestamp columns for start_time and end_time with default values
  - Decimal column for num_time with precision 4, scale 2 for hour calculations
  - String column for shuttles JSON storage
  - State column with default 'Started' for lifecycle management
  - Indexes on foreign key columns for performance optimization
- Initialization scripts:
  - Insert default courts during QA provisioning

```mermaid
flowchart TD
Start(["Startup"]) --> CheckLb["Liquibase checks applied changesets"]
CheckLb --> ApplyQA["Apply badminton-qa.sql"]
ApplyQA --> ApplyRoot["Apply root-user.sql"]
ApplyRoot --> ApplyCourts["Apply court-creation.sql"]
ApplyCourts --> ApplyRent["Apply rent-by-time.sql (NEW)"]
ApplyRent --> Done(["Schema Ready"])
```

**Diagram sources**
- [db.changelog-master.xml:7-18](file://BadmintonCourtManagement/src/main/resources/db/changelog/db.changelog-master.xml#L7-L18)
- [badminton-qa.sql:18-220](file://BadmintonCourtManagement/src/main/resources/db/changelog/badminton-qa.sql#L18-L220)
- [court-creation.sql:1-3](file://BadmintonCourtManagement/src/main/resources/db/changelog/court-creation.sql#L1-L3)
- [rent-by-time.sql:5-21](file://BadmintonCourtManagement/src/main/resources/db/changelog/rent-by-time.sql#L5-L21)

**Section sources**
- [db.changelog-master.xml:1-20](file://BadmintonCourtManagement/src/main/resources/db/changelog/db.changelog-master.xml#L1-L20)
- [badminton-qa.sql:18-220](file://BadmintonCourtManagement/src/main/resources/db/changelog/badminton-qa.sql#L18-L220)
- [court-creation.sql:1-3](file://BadmintonCourtManagement/src/main/resources/db/changelog/court-creation.sql#L1-L3)
- [rent-by-time.sql:1-21](file://BadmintonCourtManagement/src/main/resources/db/changelog/rent-by-time.sql#L1-L21)

### JPA Entity Relationships and Cascade Operations
- Session to AvailablePlayer: one-to-many with cascade-all; ensures availability records persist with session lifecycle
- Player to AvailablePlayer: many-to-one; cascading deletes propagate to availability records
- Court to Game: one-to-many; games reference courts via foreign key
- Court to RentByTime: one-to-many; rentals reference courts via foreign key
- Game to Team: one-to-one for both teams with cascade-all; orphan removal for cleanup
- Game to GameShuttleMap: one-to-many with cascade-all and orphan removal
- ShuttleBall to GameShuttleMap: one-to-one with cascade-detach to decouple shuttle updates from game lifecycle
- Team to AvailablePlayer: many-to-one relationships for player composition with expense tracking
- AvailablePlayer to RentByTime: one-to-many; player rentals tracked by availability record
- RentByTime to AvailablePlayer: many-to-one; rental references player availability
- RentByTime to Court: many-to-one; rental references court booking

```mermaid
erDiagram
PLAYER ||--o{ AVAILABLE_PLAYER : "has"
SESSION ||--o{ AVAILABLE_PLAYER : "hosts"
COURT ||--o{ GAME : "bookings"
COURT ||--o{ RENT_BY_TIME : "rentals"
GAME ||--|| TEAM : "teamOne/teamTwo"
SHUTTLE_BALL ||--o{ GAME_SHUTTLE_MAP : "used_in"
GAME ||--o{ GAME_SHUTTLE_MAP : "tracks"
TEAM ||--|| GAME : "belongs_to"
TEAM ||--o{ AVAILABLE_PLAYER : "composition"
AVAILABLE_PLAYER ||--o{ TEAM : "member_of"
AVAILABLE_PLAYER ||--o{ RENT_BY_TIME : "creates"
RENT_BY_TIME ||--|| COURT : "booked_at"
```

**Diagram sources**
- [Player.java:15-39](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Player.java#L15-L39)
- [AvailablePlayer.java:12-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L12-L58)
- [Session.java:17-41](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Session.java#L17-L41)
- [Court.java:11-43](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Court.java#L11-L43)
- [Game.java:13-79](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L13-L79)
- [Team.java:8-40](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Team.java#L8-L40)
- [ShuttleBall.java:11-62](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/ShuttleBall.java#L11-L62)
- [GameShuttleMap.java:7-31](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/GameShuttleMap.java#L7-L31)
- [RentByTime.java:21-42](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java#L21-L42)

**Section sources**
- [Session.java:35-36](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Session.java#L35-L36)
- [AvailablePlayer.java:22-28](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L22-L28)
- [Game.java:23-42](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L23-L42)
- [Game.java:33-34](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L33-L34)
- [GameShuttleMap.java:15-21](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/GameShuttleMap.java#L15-L21)
- [Team.java:18-33](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Team.java#L18-L33)
- [RentByTime.java:21-42](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java#L21-L42)

### Repository Pattern Implementation
- SessionRepository
  - Derived queries for active sessions within time windows
  - Pessimistic locking for concurrent access control
  - Parameterized filtering with SessionParam
  - Range queries with pagination and counting variants
  - Year/Month aggregation for reporting
- GameRepository
  - Find active games by court and state
  - Fetch games with eager joins for teams and court
  - Filter by player availability identifiers
  - Specialized query for player-based game retrieval
- AvailablePlayerRepository
  - Locking reads for session availability
  - Selective updates with pessimistic write locks
  - Exclusion lists and presence filters
- TeamRepository (New)
  - Standard CRUD operations for team entities
  - Supports team composition management
- UserRepository (New)
  - User authentication and management
  - Username-based lookup and user listing
- RentByTimeRepository (New)
  - **New** Find active rental by court and state
  - **New** Find rentals by player availability ID
  - **New** Find rentals by court ID
  - **New** Find rentals by state for reporting

```mermaid
sequenceDiagram
participant Repo as "RentByTimeRepository"
participant DB as "Database"
Repo->>DB : "findByCourtCourtIdAndState(courtId, state)"
DB-->>Repo : "Optional<RentByTime>"
Repo->>DB : "findByAvailablePlayerAvaId(avaId)"
DB-->>Repo : "List<RentByTime>"
Repo->>DB : "findByCourtCourtId(courtId)"
DB-->>Repo : "List<RentByTime>"
Repo->>DB : "findByState(state)"
DB-->>Repo : "List<RentByTime>"
```

**Diagram sources**
- [RentByTimeRepository.java:13-19](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/RentByTimeRepository.java#L13-L19)

**Section sources**
- [SessionRepository.java:19-55](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L19-L55)
- [GameRepository.java:13-32](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/GameRepository.java#L13-L32)
- [AvailablePlayerRepository.java:15-35](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L15-L35)
- [TeamRepository.java:1-10](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/TeamRepository.java#L1-L10)
- [UserRepository.java:1-16](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/UserRepository.java#L1-L16)
- [RentByTimeRepository.java:10-20](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/RentByTimeRepository.java#L10-L20)
- [SessionParam.java:8-14](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/filter/SessionParam.java#L8-L14)

### Transaction Management and Concurrency Control
- Pessimistic locking is used in repositories to prevent race conditions when checking availability or updating records
- Lock hints specify timeout behavior for long-running queries
- Repository methods annotated with locking semantics ensure consistent reads/writes under contention
- Service layer transactions wrap complex reporting operations
- **Updated** RentByTimeService methods are transactional for atomic rental operations

**Section sources**
- [SessionRepository.java:22-24](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L22-L24)
- [AvailablePlayerRepository.java:17-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L17-L34)
- [RentByTimeService.java:63-161](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L63-L161)

### Session-Centric Data Storage and Historical Preservation
- Sessions capture time windows and activity flags
- AvailablePlayer links players to sessions, capturing join/leave timestamps and payment attributes
- Enhanced team composition tracking with individual expense allocation
- **Updated** RentByTime system captures hourly court rentals with shuttle ball management
- Historical insights:
  - Session range queries enable monthly aggregation
  - Game state and ended date support lifecycle analytics
  - Shuttle usage tracked per game via GameShuttleMap
  - Expense tracking supports financial reporting and analysis
  - **New** Rental state tracking supports hourly booking analytics
  - **New** Rental duration calculations support revenue analysis

```mermaid
flowchart TD
S(["Session Open"]) --> Join["Player Joins (AvailablePlayer)"]
Join --> Play["Game Created"]
Play --> End["Game Ended (endedDate set)"]
End --> Expense["Expense Allocation (Team.expenseOne/Two)"]
Expense --> Leave["Player Leaves (leaveTime set)"]
Leave --> Close["Session Close (toTime set)"]
Close --> Archive(["Historical Queries by Session Range"])
Archive --> Report["Expense Reporting & Analytics"]
RentStart["Rental Start (RentByTime)"] --> RentActive["Rental Active (State: Started)"]
RentActive --> RentUpdate["Rental Update (Duration/Equipment)"]
RentUpdate --> RentFinish["Rental Finish (State: Finish)"]
RentFinish --> RentCancel["Rental Cancel (State: Cancel)"]
RentFinish --> Archive
RentCancel --> Archive
```

**Diagram sources**
- [Session.java:27-31](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Session.java#L27-L31)
- [AvailablePlayer.java:30-31](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L30-L31)
- [Game.java:47-48](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L47-L48)
- [Team.java:22-30](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Team.java#L22-L30)
- [RentByTime.java:35-42](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java#L35-L42)
- [SessionRepository.java:53-54](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L53-L54)

**Section sources**
- [SessionRepository.java:29-54](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L29-L54)
- [Game.java:50-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L50-L58)
- [RentByTimeService.java:63-161](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L63-L161)

### Typical Data Access Scenarios and Query Optimization
- Find active sessions overlapping current time with pessimistic read lock
- Filter sessions by inclusive start and exclusive end bounds with pagination
- Count sessions within a time range for reporting
- Retrieve games by court with optional ended-date null predicate
- Fetch games with joined entities to avoid N+1 selects
- Locate players currently present in a session with locking for update
- Aggregate distinct year/month from session timestamps for reporting
- Calculate total expenses for session-based reporting
- Extract individual player expenses from team composition data
- **New** Find active rental by court and state for current booking management
- **New** Retrieve player's rental history by availability ID
- **New** Get court's rental schedule by court ID
- **New** Filter rentals by state for reporting and analytics

Optimization techniques:
- Use JOIN FETCH to eagerly load associated entities where needed
- Prefer indexed columns in WHERE clauses (e.g., from_time, to_time, ended_date, rental state)
- Apply pagination and range queries to limit result sets
- Employ pessimistic locking only where necessary to minimize contention
- Utilize specialized repository methods for complex aggregations
- **New** Index foreign key columns (ava_id, court_id) for rental queries
- **New** Use state-based filtering for efficient rental lifecycle queries

**Section sources**
- [SessionRepository.java:20-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L20-L34)
- [SessionRepository.java:35-41](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L35-L41)
- [SessionRepository.java:46-54](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L46-L54)
- [GameRepository.java:18-30](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/GameRepository.java#L18-L30)
- [AvailablePlayerRepository.java:29-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L29-L34)
- [RentByTimeRepository.java:13-19](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/RentByTimeRepository.java#L13-L19)
- [ExcelExportService.java:631-646](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java#L631-L646)

## Enhanced Querying Capabilities

### Advanced Expense Tracking Queries
The system now supports sophisticated expense tracking through enhanced Team entities with dedicated expense columns:

- Individual player expense extraction from team composition
- Team win/loss status tracking for performance analysis
- Revenue calculation across multiple sessions and players
- Expense distribution algorithms for team-based cost allocation

```mermaid
flowchart TD
Expense["Player Expense Query"] --> TeamLookup["Find Team by Player ID"]
TeamLookup --> ExpenseOne["Check Team.expenseOne"]
ExpenseOne --> ExpenseTwo["Check Team.expenseTwo"]
ExpenseTwo --> Result["Return Matching Expense Value"]
```

**Diagram sources**
- [ExcelExportService.java:631-646](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java#L631-L646)
- [Team.java:22-30](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Team.java#L22-L30)

### Team Composition Analysis
Enhanced team relationships enable comprehensive composition analysis:

- Player pairing analysis for team formation patterns
- Expense distribution tracking across team members
- Win/loss statistics by team composition
- Performance metrics based on team dynamics

**Section sources**
- [Team.java:18-33](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Team.java#L18-L33)
- [ExcelExportService.java:582-598](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java#L582-L598)

## Advanced Reporting and Analytics

### Comprehensive Expense Reporting
The ExcelExportService provides advanced reporting capabilities:

- Multi-session expense aggregation
- Team composition-based expense allocation
- Revenue tracking across player activities
- Service usage analysis beyond court fees

```mermaid
sequenceDiagram
participant Service as "ExcelExportService"
participant Repo as "Repositories"
participant Report as "Report Generation"
Service->>Repo : "retrieveReport(sessionId)"
Repo-->>Service : "Session, Players, Games"
Service->>Service : "calculateExpenses()"
Service->>Service : "aggregateRevenue()"
Service->>Report : "buildReportModel()"
Report-->>Service : "ExportReady"
```

**Diagram sources**
- [ExcelExportService.java:648-656](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java#L648-L656)
- [ExcelExportService.java:658-662](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java#L658-L662)

### Revenue Calculation and Analysis
Advanced revenue tracking mechanisms:

- Gross revenue calculation from player payments
- Individual player expense tracking per game
- Team-based cost allocation algorithms
- Service usage revenue integration
- **New** Hourly rental revenue calculation based on duration and rate

**Section sources**
- [ExcelExportService.java:616-618](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java#L616-L618)
- [ExcelExportService.java:631-646](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java#L631-L646)
- [ExcelExportService.java:687-708](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java#L687-L708)

## User Management Patterns

### Authentication and Authorization Infrastructure
The system now includes comprehensive user management:

- Player entity with authentication credentials
- UserRepository for user operations
- Password security considerations
- User-based access patterns

```mermaid
classDiagram
class Player {
+int playerId
+String playerName
+String password
+Timestamp createdDate
}
class UserRepository {
<<interface>>
+findByPlayerName(username)
+findAllByPlayerName(name)
}
Player "1" --> "0..*" UserRepository : "managed_by"
```

**Diagram sources**
- [Player.java:15-39](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Player.java#L15-L39)
- [UserRepository.java:1-16](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/UserRepository.java#L1-16)

### User-Based Data Access Patterns
User management enables role-based data access:

- Player-specific availability tracking
- Personal expense history
- Team composition analysis by user
- Access control through user authentication
- **New** Player-specific rental history tracking

**Section sources**
- [Player.java:25-35](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Player.java#L25-L35)
- [UserRepository.java:12-14](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/UserRepository.java#L12-L14)

## Rent-by-Time System Integration

### Comprehensive Rental Lifecycle Management
The RentByTime system provides complete hourly court rental functionality:

- **Entity Design**: RentByTime entity with comprehensive fields for rental tracking
- **Repository Methods**: Specialized queries for court availability, player history, and state management
- **Service Operations**: Full CRUD operations with state transitions and fee calculations
- **API Integration**: Request/response models for seamless client integration
- **Business Logic**: Hourly rate calculation, duration tracking, and equipment management

```mermaid
sequenceDiagram
participant Controller as "CourtManagementController"
participant Service as "RentByTimeService"
participant Repo as "RentByTimeRepository"
participant DB as "Database"
Controller->>Service : "applyRentByTime(request)"
Service->>Service : "validate court and player"
Service->>Repo : "findByCourtCourtIdAndState(courtId, 'Started')"
Repo-->>Service : "Optional<RentByTime>"
Service->>DB : "save(new RentByTime)"
DB-->>Service : "RentByTime saved"
Service->>Controller : "RentByTimeResponse"
```

**Diagram sources**
- [CourtManagementController.java:176-214](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java#L176-L214)
- [RentByTimeService.java:63-161](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L63-L161)
- [RentByTimeRepository.java:13](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/RentByTimeRepository.java#L13)

### Rental State Management and Fee Calculation
The system implements comprehensive state management for rental lifecycle:

- **State Transitions**: Started → Finish → Cancel states with proper validation
- **Fee Calculation**: Based on hourly rate from Service entity with fallback default
- **Duration Tracking**: Automatic calculation of rental duration in minutes
- **Equipment Management**: Shuttle ball tracking through JSON serialization
- **Real-time Updates**: Dynamic remaining time calculation based on current time

**Section sources**
- [RentByTimeService.java:35-61](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L35-L61)
- [RentByTimeService.java:163-255](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L163-L255)
- [RentByTime.java:44-54](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java#L44-L54)

### API Integration and Client Communication
The RentByTime system provides comprehensive API endpoints:

- **Apply Rental**: Create new hourly court reservations
- **Pay Rental**: Complete rental payment and finalize booking
- **Cancel Rental**: Cancel active rentals with proper state management
- **Update Rental**: Modify rental details including duration and equipment
- **Get Active Rental**: Retrieve current active rental for a court
- **Get Current Time**: Server-side time synchronization for accurate calculations

**Section sources**
- [CourtManagementController.java:176-214](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java#L176-L214)
- [RentByTimeRequest.java:11-21](file://BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/RentByTimeRequest.java#L11-L21)
- [RentByTimeResponse.java:12-24](file://BadmintonCourtManagement/src/main/java/com/badminton/response/RentByTimeResponse.java#L12-L24)

## Database Schema Evolution

### Enhanced Schema with RentByTime Integration
The database schema has evolved to support comprehensive rental operations:

- **New Table**: rent_by_time table with complete foreign key relationships
- **Foreign Keys**: References to available_player and court tables
- **Indexes**: Optimized foreign key indexes for query performance
- **Constraints**: Proper referential integrity enforcement
- **Default Values**: Appropriate defaults for timestamp and state fields
- **Precision Handling**: Decimal precision for accurate hourly billing

```mermaid
erDiagram
RENT_BY_TIME {
int id PK
bigint ava_id FK
int court_id FK
timestamp start_time
timestamp end_time
decimal num_time
varchar shuttles
varchar state
}
AVAILABLE_PLAYER ||--o{ RENT_BY_TIME : "creates"
COURT ||--o{ RENT_BY_TIME : "booked_at"
```

**Diagram sources**
- [rent-by-time.sql:5-21](file://BadmintonCourtManagement/src/main/resources/db/changelog/rent-by-time.sql#L5-L21)
- [RentByTime.java:21-42](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java#L21-L42)

### Migration Strategy and Backward Compatibility
The migration strategy ensures smooth integration of new rental functionality:

- **Liquibase Integration**: New changeset in master changelog for schema evolution
- **Backward Compatibility**: Existing schema remains intact with new additions
- **Data Integrity**: Foreign key constraints maintain referential integrity
- **Performance Optimization**: Indexes on foreign key columns for query optimization
- **Default Configuration**: Appropriate defaults ensure immediate functionality

**Section sources**
- [db.changelog-master.xml:16-18](file://BadmintonCourtManagement/src/main/resources/db/changelog/db.changelog-master.xml#L16-L18)
- [rent-by-time.sql:16-20](file://BadmintonCourtManagement/src/main/resources/db/changelog/rent-by-time.sql#L16-L20)

## Dependency Analysis
External dependencies supporting persistence and enhanced features:
- Spring Data JPA for repository abstractions
- Hibernate ORM for JPA provider
- Liquibase for declarative schema management
- MySQL Connector/J for database connectivity
- Apache POI for advanced reporting capabilities
- Lombok for code generation and boilerplate reduction
- **Updated** Jackson for JSON serialization in rental operations

```mermaid
graph LR
App["Application"] --> SDJ["Spring Data JPA"]
App --> HIB["Hibernate"]
App --> LB["Liquibase"]
App --> MYSQL["MySQL Connector/J"]
App --> POI["Apache POI (Reporting)"]
App --> LOMBOK["Lombok (Code Gen)"]
App --> JACKSON["Jackson (JSON)"]
```

**Diagram sources**
- [pom.xml:37-112](file://BadmintonCourtManagement/pom.xml#L37-L112)

**Section sources**
- [pom.xml:37-112](file://BadmintonCourtManagement/pom.xml#L37-L112)

## Performance Considerations
- Use pagination-aware queries for large datasets (e.g., SessionRepository range queries)
- Leverage JOIN FETCH judiciously to reduce lazy-loading overhead
- Index foreign keys and frequently queried columns (session timestamps, game ended date, rental state)
- Apply pessimistic locks only for critical sections to avoid blocking
- Monitor SQL logs and adjust queries based on execution plans
- Optimize reporting queries with specialized repository methods
- Cache frequently accessed user and team data
- Implement efficient expense calculation algorithms
- **New** Index rental foreign key columns (ava_id, court_id) for optimal query performance
- **New** Use state-based filtering for efficient rental lifecycle queries
- **New** Implement proper JSON serialization/deserialization for shuttle equipment data

## Troubleshooting Guide
Common issues and resolutions:
- Liquibase parsing warnings: secure parsing disabled in configuration; verify schema changesets are applied
- Schema mismatch after refactor: ensure foreign keys and indexes remain intact; re-run Liquibase
- Deadlocks on concurrent availability updates: review locking strategies and reduce lock scope
- N+1 select problems: confirm JOIN FETCH usage in repository queries
- Expense calculation discrepancies: verify team composition and expense allocation logic
- User authentication failures: check password encoding and user validation processes
- Reporting performance issues: optimize aggregation queries and implement appropriate indexing
- **New** Rental state conflicts: verify proper state transitions and concurrent access handling
- **New** Rental duration calculation errors: check time zone handling and UTC conversion
- **New** Shuttle equipment serialization issues: validate JSON format and deserialization logic
- **New** Rental query performance: ensure foreign key indexes are utilized effectively

**Section sources**
- [application.properties:18-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L18-L19)
- [GameRepository.java:24-30](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/GameRepository.java#L24-L30)
- [ExcelExportService.java:631-646](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java#L631-L646)
- [RentByTimeService.java:63-161](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L63-L161)

## Conclusion
The enhanced persistence layer combines robust JPA entities, Spring Data repositories, and Liquibase migrations to support session-centric operations, player tracking, team composition analysis, comprehensive expense tracking, and complete rent-by-time functionality. The addition of user management capabilities, advanced reporting features, and comprehensive rental lifecycle management provides a complete solution for financial tracking, operational analytics, and hourly court reservation management. Proper indexing, locking, pagination, and transaction management ensure scalability and reliability for enterprise-level usage with full backward compatibility and smooth schema evolution.

## Appendices
- Configuration highlights:
  - Hibernate dialect and SQL logging enabled
  - Liquibase logging configured
  - Profiles for dev/qa/prod environments
  - Apache POI dependencies for reporting
  - Lombok annotation processing enabled
  - **New** Jackson dependency for JSON serialization in rental operations

**Section sources**
- [application.properties:4-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L4-L19)
- [pom.xml:37-112](file://BadmintonCourtManagement/pom.xml#L37-L112)