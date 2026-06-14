# RentByTime System

<cite>
**Referenced Files in This Document**
- [RentByTime.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java)
- [RentByTimeRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/RentByTimeRepository.java)
- [RentByTimeRequest.java](file://BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/RentByTimeRequest.java)
- [RentByTimeResponse.java](file://BadmintonCourtManagement/src/main/java/com/badminton/response/RentByTimeResponse.java)
- [RentByTimeService.java](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java)
- [CourtManagementController.java](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java)
- [RentByTimeDialog.js](file://bad-court-mana-ui/src/page/dialog/RentByTimeDialog.js)
- [rent-by-time.sql](file://BadmintonCourtManagement/src/main/resources/db/changelog/rent-by-time.sql)
- [GameState.java](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/GameState.java)
- [GameConstant.java](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/GameConstant.java)
- [PayType.java](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/PayType.java)
- [ApiConstant.java](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/ApiConstant.java)
- [CommonConstant.java](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/CommonConstant.java)
- [ErrorConstant.java](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/ErrorConstant.java)
- [SessionServiceImpl.java](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/SessionServiceImpl.java)
- [AvailablePlayer.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java)
- [Court.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Court.java)
- [Player.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Player.java)
- [ServiceUtil.java](file://BadmintonCourtManagement/src/main/java/com/badminton/util/ServiceUtil.java)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [System Architecture](#system-architecture)
3. [Core Components](#core-components)
4. [RentByTime Entity Model](#rentbytime-entity-model)
5. [Service Layer Implementation](#service-layer-implementation)
6. [API Endpoints](#api-endpoints)
7. [UI Integration](#ui-integration)
8. [Database Schema](#database-schema)
9. [Business Logic Flow](#business-logic-flow)
10. [Error Handling](#error-handling)
11. [Performance Considerations](#performance-considerations)
12. [Conclusion](#conclusion)

## Introduction

The RentByTime System is a specialized module within the Badminton Court Management platform designed to handle time-based court rentals. This system enables customers to rent badminton courts for specific time periods, track rental status, manage payments, and monitor remaining time usage. The system integrates seamlessly with the broader court management infrastructure while providing dedicated functionality for time-based rentals.

The module supports real-time rental operations including initiation, payment processing, cancellation, updates, and status monitoring. It maintains accurate time tracking, calculates fees based on hourly rates, manages shuttle ball allocations, and provides comprehensive reporting capabilities for rental activities.

## System Architecture

The RentByTime System follows a layered architecture pattern with clear separation of concerns across different layers:

```mermaid
graph TB
subgraph "Presentation Layer"
UI[React Frontend]
Dialog[RentByTimeDialog]
end
subgraph "Controller Layer"
Controller[CourtManagementController]
end
subgraph "Service Layer"
Service[RentByTimeService]
Calc[GameExpenseCalculator]
end
subgraph "Repository Layer"
Repo[RentByTimeRepository]
PlayerRepo[AvailablePlayerRepository]
end
subgraph "Persistence Layer"
DB[(Database)]
Entities[RentByTime Entity]
end
UI --> Dialog
Dialog --> Controller
Controller --> Service
Service --> Repo
Service --> PlayerRepo
Repo --> DB
PlayerRepo --> DB
DB --> Entities
```

**Diagram sources**
- [CourtManagementController.java:176-214](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java#L176-L214)
- [RentByTimeService.java:73-255](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L73-L255)

The architecture ensures loose coupling between components while maintaining clear data flow from user interface to database persistence. The service layer encapsulates business logic, while repositories handle data access operations.

**Section sources**
- [CourtManagementController.java:176-214](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java#L176-L214)
- [RentByTimeService.java:73-255](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L73-L255)

## Core Components

### Entity Layer

The RentByTime entity serves as the core data structure representing rental transactions:

```mermaid
classDiagram
class RentByTime {
+Integer id
+AvailablePlayer availablePlayer
+Court court
+Instant startTime
+Instant endTime
+BigDecimal numTime
+String shuttles
+String state
+Instant createdAt
+Instant updatedAt
}
class AvailablePlayer {
+Integer id
+Player player
+String currentServices
+String availability
}
class Court {
+Integer id
+String courtName
+Float hourlyRate
+String location
}
class Player {
+Integer id
+String playerName
+String email
+String phone
}
RentByTime --> AvailablePlayer : "belongs to"
AvailablePlayer --> Player : "contains"
RentByTime --> Court : "uses"
```

**Diagram sources**
- [RentByTime.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java)
- [AvailablePlayer.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java)
- [Court.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Court.java)
- [Player.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/Player.java)

### Request and Response Models

The system utilizes dedicated DTOs for request and response handling:

```mermaid
classDiagram
class RentByTimeRequest {
+Integer playerId
+Integer courtId
+Float numTime
+ShuttleBallDTO[] shuttleBalls
}
class RentByTimeResponse {
+Integer id
+String playerName
+String courtName
+Instant startTime
+Instant endTime
+Float numTime
+Float fee
+ShuttleBallDTO[] shuttleBalls
+String state
+Integer remainingMinutes
}
RentByTimeRequest --> RentByTimeResponse : "maps to"
```

**Diagram sources**
- [RentByTimeRequest.java](file://BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/RentByTimeRequest.java)
- [RentByTimeResponse.java](file://BadmintonCourtManagement/src/main/java/com/badminton/response/RentByTimeResponse.java)

**Section sources**
- [RentByTime.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java)
- [RentByTimeRequest.java](file://BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/RentByTimeRequest.java)
- [RentByTimeResponse.java](file://BadmintonCourtManagement/src/main/java/com/badminton/response/RentByTimeResponse.java)

## RentByTime Entity Model

The RentByTime entity encapsulates all essential attributes for time-based court rentals:

### Core Attributes

| Attribute | Type | Description | Constraints |
|-----------|------|-------------|-------------|
| id | Integer | Unique identifier | Primary Key |
| availablePlayer | AvailablePlayer | Player associated with rental | Foreign Key |
| court | Court | Court being rented | Foreign Key |
| startTime | Instant | Rental start timestamp | Required |
| endTime | Instant | Rental end timestamp | Required |
| numTime | BigDecimal | Duration in hours | Precision: 2 decimal places |
| shuttles | String | JSON serialized shuttle balls | JSON format |
| state | String | Current rental state | Enumerated values |

### State Management

The system implements a finite state machine for rental lifecycle management:

```mermaid
stateDiagram-v2
[*] --> STARTED
STARTED --> PAID : "payment processed"
PAID --> FINISHED : "end time reached"
PAID --> CANCELLED : "cancellation requested"
FINISHED --> [*]
CANCELLED --> [*]
```

**Diagram sources**
- [GameState.java](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/GameState.java)

**Section sources**
- [RentByTime.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java)
- [GameState.java](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/GameState.java)

## Service Layer Implementation

### Business Logic Implementation

The RentByTimeService handles all business operations for time-based rentals:

```mermaid
sequenceDiagram
participant Client as "Client Application"
participant Controller as "CourtManagementController"
participant Service as "RentByTimeService"
participant Repo as "RentByTimeRepository"
participant PlayerRepo as "AvailablePlayerRepository"
participant DB as "Database"
Client->>Controller : POST /applyRentByTime
Controller->>Service : applyRentByTime(request)
Service->>Service : calculateEndTime()
Service->>Service : buildShuttlesJson()
Service->>Repo : save(rental)
Repo->>DB : INSERT operation
DB-->>Repo : rental saved
Repo-->>Service : rental object
Service->>Service : addServiceToPlayer()
Service->>PlayerRepo : save(player)
PlayerRepo->>DB : UPDATE operation
DB-->>PlayerRepo : player updated
PlayerRepo-->>Service : success
Service-->>Controller : RentByTimeResponse
Controller-->>Client : ResponseEntity
```

**Diagram sources**
- [CourtManagementController.java:176-180](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java#L176-L180)
- [RentByTimeService.java:73-94](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L73-L94)

### Key Service Methods

The service layer implements several critical methods for rental management:

1. **applyRentByTime**: Creates new rental applications
2. **payRentByTime**: Processes payment for active rentals
3. **cancelRentByTime**: Handles rental cancellations
4. **updateRentByTime**: Modifies existing rental terms
5. **getActiveRentByTimeForCourt**: Retrieves current rentals

**Section sources**
- [RentByTimeService.java:73-255](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L73-L255)

## API Endpoints

The RentByTime module exposes RESTful endpoints for comprehensive rental management:

### Endpoint Definitions

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| POST | `/applyRentByTime` | Create new rental application | RentByTimeRequest body |
| POST | `/payRentByTime` | Process rental payment | rentId (path), customFee (optional) |
| POST | `/cancelRentByTime` | Cancel active rental | rentId (path) |
| POST | `/updateRentByTime` | Modify rental terms | rentId (path), RentByTimeRequest body |
| GET | `/getActiveRentByTime` | Get current rental | courtId (query) |
| GET | `/getCurrentTime` | Get server time | None |

### Request/Response Examples

**Apply Rent Request:**
```json
{
  "playerId": 1,
  "courtId": 5,
  "numTime": 2.5,
  "shuttleBalls": [
    {"name": "Bull Hide", "quantity": 3}
  ]
}
```

**Payment Response:**
```json
{
  "id": 101,
  "playerName": "John Doe",
  "courtName": "Court 1",
  "startTime": "2024-01-15T14:00:00Z",
  "endTime": "2024-01-15T16:30:00Z",
  "numTime": 2.5,
  "fee": 250000.0,
  "shuttleBalls": [...],
  "state": "PAID",
  "remainingMinutes": 150
}
```

**Section sources**
- [CourtManagementController.java:176-214](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java#L176-L214)

## UI Integration

### React Component Architecture

The frontend integration utilizes a modal dialog system for seamless user interaction:

```mermaid
graph TB
subgraph "UI Components"
Dialog[RentByTimeDialog]
Form[Form Controls]
Timer[Timer Display]
Shuttle[Shuttle Selection]
end
subgraph "State Management"
StartH[Start Hour]
StartM[Start Minute]
DurH[Duration Hour]
DurM[Duration Minute]
Fee[Fee Calculator]
Shuttles[Shuttle List]
end
subgraph "Event Handlers"
Confirm[onConfirm Handler]
Exit[onExit Handler]
end
Dialog --> Form
Dialog --> Timer
Dialog --> Shuttle
Form --> StartH
Form --> StartM
Form --> DurH
Form --> DurM
Form --> Fee
Form --> Shuttles
Dialog --> Confirm
Dialog --> Exit
```

**Diagram sources**
- [RentByTimeDialog.js:77-102](file://bad-court-mana-ui/src/page/dialog/RentByTimeDialog.js#L77-L102)

### User Interaction Flow

The UI provides intuitive controls for rental management:

1. **Time Configuration**: Users can set start time, duration, and automatically calculated end time
2. **Fee Calculation**: Real-time fee calculation based on hourly rate and duration
3. **Shuttle Selection**: Dropdown selection for shuttle ball types and quantities
4. **Confirmation**: Modal-based confirmation with validation

**Section sources**
- [RentByTimeDialog.js:77-102](file://bad-court-mana-ui/src/page/dialog/RentByTimeDialog.js#L77-L102)

## Database Schema

### Table Structure

The RentByTime functionality relies on a well-designed database schema:

```mermaid
erDiagram
RENT_BY_TIME {
integer id PK
integer available_player_id FK
integer court_id FK
timestamp start_time
timestamp end_time
decimal num_time
json shuttles
varchar state
timestamp created_at
timestamp updated_at
}
AVAILABLE_PLAYER {
integer id PK
integer player_id FK
json current_services
varchar availability
}
COURT {
integer id PK
varchar court_name
float hourly_rate
varchar location
}
PLAYER {
integer id PK
varchar player_name
varchar email
varchar phone
}
RENT_BY_TIME }o--|| AVAILABLE_PLAYER : "belongs_to"
AVAILABLE_PLAYER }o--|| PLAYER : "contains"
RENT_BY_TIME }o--|| COURT : "uses"
```

**Diagram sources**
- [rent-by-time.sql](file://BadmintonCourtManagement/src/main/resources/db/changelog/rent-by-time.sql)

### Schema Evolution

The database schema supports flexible data types for dynamic shuttle ball management and maintains referential integrity through foreign key constraints. The JSON field for shuttles allows for scalable storage of equipment data without schema modifications.

**Section sources**
- [rent-by-time.sql](file://BadmintonCourtManagement/src/main/resources/db/changelog/rent-by-time.sql)

## Business Logic Flow

### Rental Lifecycle Management

The system implements comprehensive business logic for rental operations:

```mermaid
flowchart TD
Start([Rental Initiation]) --> ValidateInput["Validate Player & Court"]
ValidateInput --> InputValid{"Valid Input?"}
InputValid --> |No| ReturnError["Return Validation Error"]
InputValid --> |Yes| CalculateTime["Calculate Start & End Time"]
CalculateTime --> BuildShuttles["Build Shuttle JSON"]
BuildShuttles --> CreateRental["Create RentByTime Entity"]
CreateRental --> SaveRental["Save to Database"]
SaveRental --> AddService["Add Service to Player"]
AddService --> UpdatePlayer["Update Player Services"]
UpdatePlayer --> ReturnSuccess["Return Success Response"]
ReturnError --> End([End])
ReturnSuccess --> End
style Start fill:#e1f5fe
style End fill:#ffebee
style ValidateInput fill:#f3e5f5
```

**Diagram sources**
- [RentByTimeService.java:73-94](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L73-L94)

### Payment Processing Logic

Payment operations follow a secure and auditable process:

```mermaid
sequenceDiagram
participant User as "User"
participant Service as "RentByTimeService"
participant Player as "Player Repository"
participant DB as "Database"
User->>Service : payRentByTime(rentId, customFee)
Service->>Service : validateRental()
Service->>Service : calculateAmount()
Service->>Player : getPlayerById()
Player->>DB : SELECT operation
DB-->>Player : Player object
Player-->>Service : Player data
Service->>Service : updatePlayerServices()
Service->>Service : updateRentalState()
Service->>Player : save(player)
Player->>DB : UPDATE operation
DB-->>Player : success
Player-->>Service : confirmation
Service-->>User : Payment confirmation
```

**Diagram sources**
- [RentByTimeService.java:96-130](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L96-L130)

**Section sources**
- [RentByTimeService.java:73-255](file://BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java#L73-L255)

## Error Handling

### Exception Management

The system implements comprehensive error handling strategies:

| Exception Type | Trigger Condition | Response Status | Error Message |
|----------------|-------------------|-----------------|---------------|
| IllegalArgumentException | Rental not found | 400 Bad Request | "Rental not found" |
| ElementNotExistException | Player/court missing | 404 Not Found | "Entity not exists" |
| BusinessException | Business validation failure | 400 Bad Request | Specific business error |
| GlobalExceptionHandler | Unhandled exceptions | 500 Internal Server Error | Generic error message |

### Error Prevention Strategies

1. **Input Validation**: Comprehensive validation at service layer boundaries
2. **State Validation**: Ensures operations are performed in correct rental state
3. **Concurrency Control**: Prevents race conditions during rental updates
4. **Transaction Management**: Atomic operations for data consistency

**Section sources**
- [GlobalExceptionHandler.java](file://BadmintonCourtManagement/src/main/java/com/badminton/exception/GlobalExceptionHandler.java)
- [ElementNotExistException.java](file://BadmintonCourtManagement/src/main/java/com/badminton/exception/ElementNotExistException.java)

## Performance Considerations

### Optimization Strategies

1. **Database Indexing**: Strategic indexing on frequently queried columns (court_id, state, timestamps)
2. **Caching Mechanisms**: Utilization of application cache for frequently accessed configurations
3. **Connection Pooling**: Optimized database connection management
4. **Asynchronous Operations**: Non-blocking operations for time-intensive tasks

### Scalability Features

- **Horizontal Scaling**: Stateless service design supports load balancing
- **Database Optimization**: Efficient queries with proper indexing strategies
- **Memory Management**: Optimized object pooling for high-frequency operations
- **Monitoring Integration**: Built-in metrics collection for performance tracking

## Conclusion

The RentByTime System represents a comprehensive solution for managing time-based badminton court rentals. The system successfully combines modern architectural patterns with practical business requirements, providing:

- **Robust Business Logic**: Well-defined rental lifecycle management
- **User-Friendly Interface**: Intuitive React-based UI components
- **Scalable Architecture**: Layered design supporting future enhancements
- **Comprehensive Error Handling**: Fault-tolerant operations with clear error reporting
- **Performance Optimization**: Efficient database operations and caching strategies

The modular design ensures maintainability while the clear separation of concerns facilitates future extensions and modifications. The system provides a solid foundation for the broader Badminton Court Management platform, ready for production deployment and ongoing development.