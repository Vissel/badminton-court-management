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
- [SessionRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java)
- [GameRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/GameRepository.java)
- [AvailablePlayerRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java)
- [SessionParam.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/filter/SessionParam.java)
- [db.changelog-master.xml](file://BadmintonCourtManagement/src/main/resources/db/changelog/db.changelog-master.xml)
- [badminton-qa.sql](file://BadmintonCourtManagement/src/main/resources/db/changelog/badminton-qa.sql)
- [court-creation.sql](file://BadmintonCourtManagement/src/main/resources/db/changelog/court-creation.sql)
- [application.properties](file://BadmintonCourtManagement/src/main/resources/application.properties)
- [pom.xml](file://BadmintonCourtManagement/pom.xml)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)
10. [Appendices](#appendices)

## Introduction
This document explains the data management and persistence layer of the Badminton Court Management system. It covers the database schema design, JPA entity relationships, repository patterns, and Liquibase migration strategy. It also documents session-centric data storage, player tracking across sessions, historical data preservation, and practical query optimization techniques.

## Project Structure
The persistence layer is organized around:
- Entities under the entity package representing domain objects and their JPA mappings
- Repositories under the repository package implementing Spring Data JPA interfaces
- Liquibase changelogs under resources/db/changelog for schema initialization and evolution
- Application configuration for JPA/Hibernate and Liquibase behavior

```mermaid
graph TB
subgraph "Entities"
E_Session["Session.java"]
E_Game["Game.java"]
E_Player["Player.java"]
E_Team["Team.java"]
E_Court["Court.java"]
E_Shuttle["ShuttleBall.java"]
E_Avail["AvailablePlayer.java"]
E_Map["GameShuttleMap.java"]
end
subgraph "Repositories"
R_Session["SessionRepository.java"]
R_Game["GameRepository.java"]
R_Avail["AvailablePlayerRepository.java"]
end
subgraph "Liquibase"
L_Master["db.changelog-master.xml"]
L_QA["badminton-qa.sql"]
L_Courts["court-creation.sql"]
end
subgraph "Config"
C_AppProps["application.properties"]
C_Pom["pom.xml"]
end
E_Session --> E_Avail
E_Player --> E_Avail
E_Court --> E_Game
E_Game --> E_Team
E_Game --> E_Map
E_Shuttle --> E_Map
R_Session --> E_Session
R_Game --> E_Game
R_Avail --> E_Avail
L_Master --> L_QA
L_Master --> L_Courts
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
- [SessionRepository.java:19-55](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L19-L55)
- [GameRepository.java:13-32](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/GameRepository.java#L13-L32)
- [AvailablePlayerRepository.java:15-35](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L15-L35)
- [db.changelog-master.xml:1-17](file://BadmintonCourtManagement/src/main/resources/db/changelog/db.changelog-master.xml#L1-L17)
- [badminton-qa.sql:18-220](file://BadmintonCourtManagement/src/main/resources/db/changelog/badminton-qa.sql#L18-L220)
- [court-creation.sql:1-3](file://BadmintonCourtManagement/src/main/resources/db/changelog/court-creation.sql#L1-L3)
- [application.properties:1-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L1-L19)
- [pom.xml:37-112](file://BadmintonCourtManagement/pom.xml#L37-L112)

**Section sources**
- [application.properties:1-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L1-L19)
- [pom.xml:37-112](file://BadmintonCourtManagement/pom.xml#L37-L112)

## Core Components
This section documents the core entities and their relationships, highlighting JPA annotations, foreign keys, and cascade behaviors.

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

- Team
  - Identity column mapped via generated value
  - Many-to-one to AvailablePlayer for two players
  - One-to-one to Game (mapped-by)
  - Expense and win status columns

- Court
  - Identity column mapped via generated value
  - Active flag and creation timestamp
  - Collection of Games mapped by

- ShuttleBall
  - Identity column mapped via generated value
  - Name, cost, activity flags, selection flag
  - Creation timestamp

- AvailablePlayer
  - Identity column mapped via generated value
  - Many-to-one to Player and Session
  - Optional leave time, services, payment amount, and type

- GameShuttleMap
  - Identity column mapped via generated value
  - Many-to-one to Game
  - One-to-one to ShuttleBall with cascade-detach
  - Quantity field

**Section sources**
- [Session.java:17-41](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Session.java#L17-L41)
- [Game.java:13-79](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L13-L79)
- [Team.java:8-40](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Team.java#L8-L40)
- [Court.java:11-43](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Court.java#L11-L43)
- [ShuttleBall.java:11-62](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/ShuttleBall.java#L11-L62)
- [AvailablePlayer.java:12-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L12-L58)
- [GameShuttleMap.java:7-31](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/GameShuttleMap.java#L7-L31)

## Architecture Overview
The persistence architecture follows a layered design:
- Entities encapsulate domain data and relationships
- Repositories define data access contracts and derived queries
- Liquibase manages schema initialization and evolution
- Spring Data JPA and Hibernate handle persistence operations

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
Session "1" --> "*" AvailablePlayer : "mappedBy"
Player "1" --> "*" AvailablePlayer : "mappedBy"
Court "1" --> "*" Game : "mappedBy"
Game "1" --> "1..2" Team : "teamOne/teamTwo"
Game "1" --> "*" GameShuttleMap : "mappedBy"
ShuttleBall "1" --> "*" GameShuttleMap : "mappedBy"
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

## Detailed Component Analysis

### Database Schema Design and Liquibase Migration Strategy
- Master changelog orchestrates initial schema and seed data
  - Includes QA schema SQL, root user creation, and default courts
- QA schema script defines tables and constraints:
  - player, session, available_player, court, team, shuttle_ball, game, game_shuttle_map, service
  - Foreign keys enforce referential integrity across entities
  - Indexes on frequently filtered/joined columns
- Initialization scripts:
  - Insert default courts during QA provisioning

```mermaid
flowchart TD
Start(["Startup"]) --> CheckLb["Liquibase checks applied changesets"]
CheckLb --> ApplyQA["Apply badminton-qa.sql"]
ApplyQA --> ApplyRoot["Apply root-user.sql"]
ApplyRoot --> ApplyCourts["Apply court-creation.sql"]
ApplyCourts --> Done(["Schema Ready"])
```

**Diagram sources**
- [db.changelog-master.xml:7-15](file://BadmintonCourtManagement/src/main/resources/db/changelog/db.changelog-master.xml#L7-L15)
- [badminton-qa.sql:18-220](file://BadmintonCourtManagement/src/main/resources/db/changelog/badminton-qa.sql#L18-L220)
- [court-creation.sql:1-3](file://BadmintonCourtManagement/src/main/resources/db/changelog/court-creation.sql#L1-L3)

**Section sources**
- [db.changelog-master.xml:1-17](file://BadmintonCourtManagement/src/main/resources/db/changelog/db.changelog-master.xml#L1-L17)
- [badminton-qa.sql:18-220](file://BadmintonCourtManagement/src/main/resources/db/changelog/badminton-qa.sql#L18-L220)
- [court-creation.sql:1-3](file://BadmintonCourtManagement/src/main/resources/db/changelog/court-creation.sql#L1-L3)

### JPA Entity Relationships and Cascade Operations
- Session to AvailablePlayer: one-to-many with cascade-all; ensures availability records persist with session lifecycle
- Player to AvailablePlayer: many-to-one; cascading deletes propagate to availability records
- Court to Game: one-to-many; games reference courts via foreign key
- Game to Team: one-to-one for both teams with cascade-all; orphan removal for cleanup
- Game to GameShuttleMap: one-to-many with cascade-all and orphan removal
- ShuttleBall to GameShuttleMap: one-to-one with cascade-detach to decouple shuttle updates from game lifecycle

```mermaid
erDiagram
PLAYER ||--o{ AVAILABLE_PLAYER : "has"
SESSION ||--o{ AVAILABLE_PLAYER : "hosts"
COURT ||--o{ GAME : "bookings"
GAME ||--|| TEAM : "teamOne/teamTwo"
SHUTTLE_BALL ||--o{ GAME_SHUTTLE_MAP : "used_in"
GAME ||--o{ GAME_SHUTTLE_MAP : "tracks"
TEAM ||--|| GAME : "belongs_to"
AVAILABLE_PLAYER ||--o{ TEAM : "comprised_of"
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

**Section sources**
- [Session.java:35-36](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Session.java#L35-L36)
- [AvailablePlayer.java:22-28](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L22-L28)
- [Game.java:23-42](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L23-L42)
- [Game.java:33-34](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L33-L34)
- [GameShuttleMap.java:15-21](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/GameShuttleMap.java#L15-L21)

### Repository Pattern Implementation
- SessionRepository
  - Derived queries for active sessions within time windows
  - Pessimistic locking for concurrent access control
  - Parameterized filtering with SessionParam
  - Range queries with pagination and counting variants
- GameRepository
  - Find active games by court and state
  - Fetch games with eager joins for teams and court
  - Filter by player availability identifiers
- AvailablePlayerRepository
  - Locking reads for session availability
  - Selective updates with pessimistic write locks
  - Exclusion lists and presence filters

```mermaid
sequenceDiagram
participant Repo as "SessionRepository"
participant DB as "Database"
Repo->>DB : "findByFromTimeLessThanAndToTimeIsNullAndIsActive(...)"
DB-->>Repo : "List<Session> (PESSIMISTIC_READ locked)"
Repo->>DB : "findAllByParams(SessionParam, Sort)"
DB-->>Repo : "List<Session>"
Repo->>DB : "findByFromTimeBetween(start,end,pageable)"
DB-->>Repo : "Page<Session>"
```

**Diagram sources**
- [SessionRepository.java:22-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L22-L34)
- [SessionRepository.java:26-27](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L26-L27)
- [SessionRepository.java:35-41](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L35-L41)

**Section sources**
- [SessionRepository.java:19-55](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L19-L55)
- [GameRepository.java:13-32](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/GameRepository.java#L13-L32)
- [AvailablePlayerRepository.java:15-35](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L15-L35)
- [SessionParam.java:8-14](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/filter/SessionParam.java#L8-L14)

### Transaction Management and Concurrency Control
- Pessimistic locking is used in repositories to prevent race conditions when checking availability or updating records
- Lock hints specify timeout behavior for long-running queries
- Repository methods annotated with locking semantics ensure consistent reads/writes under contention

**Section sources**
- [SessionRepository.java:22-24](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L22-L24)
- [AvailablePlayerRepository.java:17-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L17-L34)

### Session-Centric Data Storage and Historical Preservation
- Sessions capture time windows and activity flags
- AvailablePlayer links players to sessions, capturing join/leave timestamps and payment attributes
- Historical insights:
  - Session range queries enable monthly aggregation
  - Game state and ended date support lifecycle analytics
  - Shuttle usage tracked per game via GameShuttleMap

```mermaid
flowchart TD
S(["Session Open"]) --> Join["Player Joins (AvailablePlayer)"]
Join --> Play["Game Created"]
Play --> End["Game Ended (endedDate set)"]
End --> Leave["Player Leaves (leaveTime set)"]
Leave --> Close["Session Close (toTime set)"]
Close --> Archive(["Historical Queries by Session Range"])
```

**Diagram sources**
- [Session.java:27-31](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Session.java#L27-L31)
- [AvailablePlayer.java:30-31](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L30-L31)
- [Game.java:47-48](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L47-L48)
- [SessionRepository.java:53-54](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L53-L54)

**Section sources**
- [SessionRepository.java:29-54](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L29-L54)
- [Game.java:50-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Game.java#L50-L58)

### Typical Data Access Scenarios and Query Optimization
- Find active sessions overlapping current time with pessimistic read lock
- Filter sessions by inclusive start and exclusive end bounds with pagination
- Count sessions within a time range for reporting
- Retrieve games by court with optional ended-date null predicate
- Fetch games with joined entities to avoid N+1 selects
- Locate players currently present in a session with locking for update
- Aggregate distinct year/month from session timestamps for reporting

Optimization techniques:
- Use JOIN FETCH to eagerly load associated entities where needed
- Prefer indexed columns in WHERE clauses (e.g., from_time, to_time, ended_date)
- Apply pagination and range queries to limit result sets
- Employ pessimistic locking only where necessary to minimize contention

**Section sources**
- [SessionRepository.java:20-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L20-L34)
- [SessionRepository.java:35-41](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L35-L41)
- [SessionRepository.java:46-54](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/SessionRepository.java#L46-L54)
- [GameRepository.java:18-30](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/GameRepository.java#L18-L30)
- [AvailablePlayerRepository.java:29-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L29-L34)

## Dependency Analysis
External dependencies supporting persistence:
- Spring Data JPA for repository abstractions
- Hibernate ORM for JPA provider
- Liquibase for declarative schema management
- MySQL Connector/J for database connectivity

```mermaid
graph LR
App["Application"] --> SDJ["Spring Data JPA"]
App --> HIB["Hibernate"]
App --> LB["Liquibase"]
App --> MYSQL["MySQL Connector/J"]
```

**Diagram sources**
- [pom.xml:37-112](file://BadmintonCourtManagement/pom.xml#L37-L112)

**Section sources**
- [pom.xml:37-112](file://BadmintonCourtManagement/pom.xml#L37-L112)

## Performance Considerations
- Use pagination-aware queries for large datasets (e.g., SessionRepository range queries)
- Leverage JOIN FETCH judiciously to reduce lazy-loading overhead
- Index foreign keys and frequently queried columns (session timestamps, game ended date)
- Apply pessimistic locks only for critical sections to avoid blocking
- Monitor SQL logs and adjust queries based on execution plans

## Troubleshooting Guide
Common issues and resolutions:
- Liquibase parsing warnings: secure parsing disabled in configuration; verify schema changesets are applied
- Schema mismatch after refactor: ensure foreign keys and indexes remain intact; re-run Liquibase
- Deadlocks on concurrent availability updates: review locking strategies and reduce lock scope
- N+1 select problems: confirm JOIN FETCH usage in repository queries

**Section sources**
- [application.properties:18-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L18-L19)
- [GameRepository.java:24-30](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/GameRepository.java#L24-L30)

## Conclusion
The persistence layer combines robust JPA entities, Spring Data repositories, and Liquibase migrations to support session-centric operations, player tracking, and historical analytics. Proper indexing, locking, and pagination ensure scalability and reliability.

## Appendices
- Configuration highlights:
  - Hibernate dialect and SQL logging enabled
  - Liquibase logging configured
  - Profiles for dev/qa/prod environments

**Section sources**
- [application.properties:4-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L4-L19)