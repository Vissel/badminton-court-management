# Payment & Financial System

<cite>
**Referenced Files in This Document**
- [PayService.java](file://BadmintonCourtManagement/src/main/java/com/badminton/service/PayService.java)
- [PayServiceImpl.java](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java)
- [ServiceTemple.java](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java)
- [PaymentController.java](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/PaymentController.java)
- [PayRequest.java](file://BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/PayRequest.java)
- [PayResponse.java](file://BadmintonCourtManagement/src/main/java/com/badminton/response/PayResponse.java)
- [AvailablePlayer.java](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java)
- [AvailablePlayerRepository.java](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java)
- [SessionServiceImpl.java](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java)
- [GameExpenseCalculator.java](file://BadmintonCourtManagement/src/main/java/com/badminton/service/calculator/GameExpenseCalculator.java)
- [MoneyUtils.java](file://BadmintonCourtManagement/src/main/java/com/badminton/util/MoneyUtils.java)
- [ServiceUtil.java](file://BadmintonCourtManagement/src/main/java/com/badminton/util/ServiceUtil.java)
- [ServiceRequest.java](file://BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/ServiceRequest.java)
- [PayType.java](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/PayType.java)
- [ProcessCallback.java](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ProcessCallback.java)
- [PayConfirm.js](file://bad-court-mana-ui/src/page/dialog/PayConfirm.js)
- [MoneyUtils.js](file://bad-court-mana-ui/src/page/MoneyUtils.js)
- [HomePage.js](file://bad-court-mana-ui/src/page/HomePage.js)
</cite>

## Update Summary
**Changes Made**
- Added comprehensive documentation for the redesigned PayConfirm dialog with banking-style UI
- Enhanced currency formatting documentation with improved VND support
- Updated user experience documentation for payment confirmation workflows
- Added frontend integration details for the modernized payment confirmation interface

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
This document explains the Payment and Financial System responsible for automated billing, payment processing, and revenue tracking. It covers the PayService implementation, the end-to-end payment workflow from total confirmation to payment completion, integration with ServiceTemple for transactional processing, billing calculation logic, currency handling with VND, and expense aggregation across games and services. It also documents payment method tracking, timestamp recording, session-based revenue consolidation, MoneyUtils utility functions, amount formatting, financial precision handling, payment confirmation dialogs, service integration patterns, error handling for payment failures, and the relationship between game completion, service usage, and automated billing generation.

**Updated** The system now features a redesigned PayConfirm dialog with a modern banking-style UI that enhances user experience for payment confirmation workflows, along with improved currency formatting support for Vietnamese Dong (VND).

## Project Structure
The payment and financial system spans several layers:
- Controller layer exposes the payment endpoint.
- Service layer implements payment processing and integrates with session management and service aggregation.
- Repository layer persists available player records with payment metadata.
- Utility and calculator layers handle formatting, JSON serialization/deserialization, and game-based expense calculations.
- Constants define payment types.
- Frontend layer provides enhanced user interface with banking-style payment confirmation dialogs.

```mermaid
graph TB
subgraph "Presentation Layer"
PC["PaymentController"]
HC["HomePage"]
PCF["PayConfirm (Frontend)"]
end
subgraph "Service Layer"
PS["PayService (interface)"]
PSI["PayServiceImpl"]
ST["ServiceTemple"]
SS["SessionServiceImpl"]
GEC["GameExpenseCalculator"]
end
subgraph "Persistence Layer"
APR["AvailablePlayerRepository"]
AP["AvailablePlayer (entity)"]
end
subgraph "Utilities"
MU["MoneyUtils"]
SU["ServiceUtil"]
MUF["MoneyUtils (Frontend)"]
end
PC --> PS
PS --> PSI
PSI --> ST
PSI --> APR
PSI --> SS
PSI --> SU
SS --> APR
GEC --> SU
MU --> PSI
MUF --> PCF
APR --> AP
HC --> PCF
```

**Diagram sources**
- [PaymentController.java:1-29](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/PaymentController.java#L1-L29)
- [PayService.java:1-12](file://BadmintonCourtManagement/src/main/java/com/badminton/service/PayService.java#L1-L12)
- [PayServiceImpl.java:1-81](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L1-L81)
- [ServiceTemple.java:1-44](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L1-L44)
- [SessionServiceImpl.java:1-322](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L1-L322)
- [AvailablePlayerRepository.java:1-35](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L1-L35)
- [AvailablePlayer.java:1-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L1-L58)
- [GameExpenseCalculator.java:1-83](file://BadmintonCourtManagement/src/main/java/com/badminton/service/calculator/GameExpenseCalculator.java#L1-L83)
- [MoneyUtils.java:1-27](file://BadmintonCourtManagement/src/main/java/com/badminton/util/MoneyUtils.java#L1-L27)
- [ServiceUtil.java:1-175](file://BadmintonCourtManagement/src/main/java/com/badminton/util/ServiceUtil.java#L1-L175)
- [PayConfirm.js:1-210](file://bad-court-mana-ui/src/page/dialog/PayConfirm.js#L1-L210)
- [MoneyUtils.js:1-21](file://bad-court-mana-ui/src/page/MoneyUtils.js#L1-L21)
- [HomePage.js:1-932](file://bad-court-mana-ui/src/page/HomePage.js#L1-L932)

**Section sources**
- [PaymentController.java:1-29](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/PaymentController.java#L1-L29)
- [PayService.java:1-12](file://BadmintonCourtManagement/src/main/java/com/badminton/service/PayService.java#L1-L12)
- [PayServiceImpl.java:1-81](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L1-L81)
- [ServiceTemple.java:1-44](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L1-L44)
- [SessionServiceImpl.java:1-322](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L1-L322)
- [AvailablePlayerRepository.java:1-35](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L1-L35)
- [AvailablePlayer.java:1-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L1-L58)
- [GameExpenseCalculator.java:1-83](file://BadmintonCourtManagement/src/main/java/com/badminton/service/calculator/GameExpenseCalculator.java#L1-L83)
- [MoneyUtils.java:1-27](file://BadmintonCourtManagement/src/main/java/com/badminton/util/MoneyUtils.java#L1-L27)
- [ServiceUtil.java:1-175](file://BadmintonCourtManagement/src/main/java/com/badminton/util/ServiceUtil.java#L1-L175)
- [PayConfirm.js:1-210](file://bad-court-mana-ui/src/page/dialog/PayConfirm.js#L1-L210)
- [MoneyUtils.js:1-21](file://bad-court-mana-ui/src/page/MoneyUtils.js#L1-L21)
- [HomePage.js:1-932](file://bad-court-mana-ui/src/page/HomePage.js#L1-L932)

## Core Components
- PaymentController: Exposes the payment endpoint and delegates to PayService.
- PayService and PayServiceImpl: Implement payment processing, validation, and persistence of payment metadata on available players.
- ServiceTemple: Provides a generic transactional wrapper around process callbacks with standardized error handling.
- SessionServiceImpl: Manages session lifecycle and provides current session context and timestamps.
- AvailablePlayer and AvailablePlayerRepository: Persist player availability, services, payment type, amount, and timestamps.
- GameExpenseCalculator: Computes per-game expenses based on shuttle usage.
- MoneyUtils: Formats amounts in Vietnamese Dong (VND).
- ServiceUtil: Serializes/deserializes service lists and supports JSON manipulation.
- PayRequest and PayResponse: Request/response DTOs for payment operations.
- PayType: Enumerates payment actions (e.g., PAY, CANCEL).
- PayConfirm (Frontend): Modern banking-style dialog for payment confirmation with enhanced user experience.
- MoneyUtils (Frontend): Improved currency formatting utilities for frontend display.

**Updated** The system now includes a redesigned PayConfirm dialog with a banking-style UI featuring colored headers, circular icons, and receipt-style service breakdowns, along with enhanced currency formatting support.

**Section sources**
- [PaymentController.java:1-29](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/PaymentController.java#L1-L29)
- [PayService.java:1-12](file://BadmintonCourtManagement/src/main/java/com/badminton/service/PayService.java#L1-L12)
- [PayServiceImpl.java:1-81](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L1-L81)
- [ServiceTemple.java:1-44](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L1-L44)
- [SessionServiceImpl.java:1-322](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L1-L322)
- [AvailablePlayer.java:1-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L1-L58)
- [AvailablePlayerRepository.java:1-35](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L1-L35)
- [GameExpenseCalculator.java:1-83](file://BadmintonCourtManagement/src/main/java/com/badminton/service/calculator/GameExpenseCalculator.java#L1-L83)
- [MoneyUtils.java:1-27](file://BadmintonCourtManagement/src/main/java/com/badminton/util/MoneyUtils.java#L1-L27)
- [ServiceUtil.java:1-175](file://BadmintonCourtManagement/src/main/java/com/badminton/util/ServiceUtil.java#L1-L175)
- [PayRequest.java:1-16](file://BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/PayRequest.java#L1-L16)
- [PayResponse.java:1-15](file://BadmintonCourtManagement/src/main/java/com/badminton/response/PayResponse.java#L1-L15)
- [PayType.java:1-9](file://BadmintonCourtManagement/src/main/java/com/badminton/constant/PayType.java#L1-L9)
- [PayConfirm.js:1-210](file://bad-court-mana-ui/src/page/dialog/PayConfirm.js#L1-L210)
- [MoneyUtils.js:1-21](file://bad-court-mana-ui/src/page/MoneyUtils.js#L1-L21)

## Architecture Overview
The payment workflow is orchestrated by the controller, validated and executed via PayServiceImpl, wrapped in ServiceTemple for consistent error handling, and persisted through AvailablePlayerRepository. SessionServiceImpl supplies the current session context and accurate timestamps. GameExpenseCalculator supports per-game expense computation, while MoneyUtils and ServiceUtil support currency formatting and JSON-based service aggregation. The frontend provides enhanced user experience through the redesigned PayConfirm dialog with banking-style UI.

```mermaid
sequenceDiagram
participant Client as "Client"
participant Controller as "PaymentController"
participant Service as "PayServiceImpl"
participant Wrapper as "ServiceTemple"
participant Session as "SessionServiceImpl"
participant Repo as "AvailablePlayerRepository"
participant Entity as "AvailablePlayer"
participant Frontend as "PayConfirm Dialog"
Client->>Controller : "POST /api/v1/pay/payToPlayer"
Controller->>Service : "payToPlayer(PayRequest)"
Service->>Wrapper : "execute(ProcessCallback)"
Wrapper->>Service : "preProcess(request)"
Service->>Session : "findListCurrentSession()"
Session-->>Service : "current session"
Service->>Service : "convert ServiceRequest to DTOs<br/>build JSON services"
Service->>Repo : "save(AvailablePlayer)"
Repo-->>Service : "persisted entity"
Service-->>Wrapper : "PayResponse"
Wrapper-->>Controller : "Result<PayResponse>"
Controller-->>Client : "HTTP 200 with Result"
Frontend->>Controller : "Payment confirmation dialog"
Frontend-->>Client : "Enhanced user experience"
```

**Diagram sources**
- [PaymentController.java:24-27](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/PaymentController.java#L24-L27)
- [PayServiceImpl.java:36-72](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L36-L72)
- [ServiceTemple.java:14-39](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L14-L39)
- [SessionServiceImpl.java:140-144](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L140-L144)
- [AvailablePlayerRepository.java:17-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L17-L34)
- [AvailablePlayer.java:1-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L1-L58)
- [PayConfirm.js:19-210](file://bad-court-mana-ui/src/page/dialog/PayConfirm.js#L19-L210)

## Detailed Component Analysis

### PayService and PayServiceImpl
- Responsibilities:
  - Validate incoming payment request (player name, service list, payment type).
  - Resolve the current session and locate the player in that session who has not yet left.
  - Convert service requests to DTOs and serialize them into a JSON array stored in the available player record.
  - Record payment type, total amount, and leave time (as an Instant in UTC+7).
  - Wrap processing in ServiceTemple for consistent success/error handling.
- Data persistence:
  - Uses AvailablePlayerRepository to fetch and save the player's availability record.
- Error handling:
  - Throws business exceptions for missing player or invalid state.
  - Relies on ServiceTemple to translate exceptions into structured Result responses.

```mermaid
classDiagram
class PayService {
+payToPlayer(payRequest) Result~PayResponse~
}
class PayServiceImpl {
-serviceTemple : ServiceTemple
-availablePlayerRepository : AvailablePlayerRepository
-sessionService : SessionServiceImpl
+payToPlayer(payRequest) Result~PayResponse~
-convertToPayResult(availablePlayer) PayResponse
}
class ServiceTemple {
+execute(callback) Result~T~
}
class AvailablePlayerRepository {
+findAvailablePlayerInSessionByName(session, playerName) Optional~AvailablePlayer~
+save(entity) AvailablePlayer
}
class SessionServiceImpl {
+findListCurrentSession() Session[]
+getUTCPlus7Instant() Instant
}
PayService <|.. PayServiceImpl
PayServiceImpl --> ServiceTemple : "uses"
PayServiceImpl --> AvailablePlayerRepository : "persists"
PayServiceImpl --> SessionServiceImpl : "context"
```

**Diagram sources**
- [PayService.java:7-11](file://BadmintonCourtManagement/src/main/java/com/badminton/service/PayService.java#L7-L11)
- [PayServiceImpl.java:28-81](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L28-L81)
- [ServiceTemple.java:13-44](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L13-L44)
- [AvailablePlayerRepository.java:15-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L15-L34)
- [SessionServiceImpl.java:41-322](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L41-L322)

**Section sources**
- [PayService.java:1-12](file://BadmintonCourtManagement/src/main/java/com/badminton/service/PayService.java#L1-L12)
- [PayServiceImpl.java:1-81](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L1-L81)
- [AvailablePlayerRepository.java:1-35](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L1-L35)
- [SessionServiceImpl.java:140-144](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L140-L144)

### ServiceTemple Transactional Wrapper
- Purpose:
  - Standardizes pre-processing, execution, and error handling across process callbacks.
  - Converts exceptions into structured Result objects with appropriate error codes and messages.
- Behavior:
  - Calls preProcess(request) before invoking process().
  - Sets success flag and data upon successful completion.
  - Captures IllegalArgumentException, BusinessException, and other Throwables with distinct error codes.

```mermaid
flowchart TD
Start(["Execute Callback"]) --> Pre["preProcess(request)"]
Pre --> Try["process()"]
Try --> Success{"Success?"}
Success --> |Yes| SetOK["Set success=true<br/>set data=process()"]
Success --> |No| Catch["Catch exceptions"]
Catch --> ArgErr{"IllegalArgumentException?"}
ArgErr --> |Yes| BadReq["Set errorCode=BAD_REQUEST<br/>errorMessage=message"]
ArgErr --> |No| BizErr{"BusinessException?"}
BizErr --> |Yes| Conflict["Set errorCode=CONFLICT<br/>errorMessage=message"]
BizErr --> |No| ServerErr["Set errorCode=INTERNAL_SERVER_ERROR<br/>errorMessage='Server error.'"]
SetOK --> Finally["Set errorMessage"]
BadReq --> Finally
Conflict --> Finally
ServerErr --> Finally
Finally --> End(["Return Result"])
```

**Diagram sources**
- [ServiceTemple.java:14-39](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L14-L39)

**Section sources**
- [ServiceTemple.java:1-44](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L1-L44)
- [ProcessCallback.java:1-13](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ProcessCallback.java#L1-L13)

### Payment Workflow: From Total Confirmation to Completion
- Endpoint: POST /api/v1/pay/payToPlayer
- Steps:
  1. Controller receives PayRequest and delegates to PayService.
  2. PayServiceImpl validates request fields and finds the current session.
  3. Service requests are converted to DTOs and serialized into a JSON array stored on the AvailablePlayer.
  4. Payment metadata (type, amount, leave time) is recorded.
  5. Persistence occurs via AvailablePlayerRepository.
  6. Response is returned through ServiceTemple's Result wrapper.

```mermaid
sequenceDiagram
participant C as "Client"
participant PC as "PaymentController"
participant PSI as "PayServiceImpl"
participant ST as "ServiceTemple"
participant SS as "SessionServiceImpl"
participant APR as "AvailablePlayerRepository"
C->>PC : "POST /api/v1/pay/payToPlayer"
PC->>PSI : "payToPlayer(PayRequest)"
PSI->>ST : "execute(ProcessCallback)"
ST->>PSI : "preProcess"
PSI->>SS : "findListCurrentSession()"
PSI->>PSI : "convert ServiceRequest to DTOs<br/>serialize to JSON"
PSI->>APR : "save(AvailablePlayer)"
APR-->>PSI : "saved entity"
PSI-->>ST : "PayResponse"
ST-->>PC : "Result<PayResponse>"
PC-->>C : "HTTP 200 OK"
```

**Diagram sources**
- [PaymentController.java:24-27](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/PaymentController.java#L24-L27)
- [PayServiceImpl.java:36-72](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L36-L72)
- [ServiceTemple.java:14-39](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L14-L39)
- [SessionServiceImpl.java:140-144](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L140-L144)
- [AvailablePlayerRepository.java:17-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L17-L34)

**Section sources**
- [PaymentController.java:1-29](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/PaymentController.java#L1-L29)
- [PayServiceImpl.java:36-72](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L36-L72)
- [ServiceTemple.java:14-39](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L14-L39)

### Billing Calculation Logic and Expense Aggregation
- Per-game expenses:
  - GameExpenseCalculator computes total ball cost from shuttle usage and splits evenly between teams depending on the outcome.
  - It builds a map of shuttle balls to quantities and multiplies by unit cost.
- Service-based charges:
  - PayServiceImpl serializes service requests into a JSON array stored on AvailablePlayer.
  - ServiceUtil supports building JSON arrays and merging/removing entries for dynamic service updates.
- Currency handling:
  - MoneyUtils provides VND formatting using Vietnamese locale.
- Precision:
  - Amounts are represented as Float in the domain and response payload.

```mermaid
flowchart TD
StartCalc(["Compute Game Expenses"]) --> BuildMap["Build Shuttle Ball Map<br/>from GameShuttleMap"]
BuildMap --> SumCost["Sum cost = Σ(shuttle.cost * count)"]
SumCost --> Outcome{"Team One Won?"}
Outcome --> |Yes| SplitTwo["Split total cost to Team Two"]
Outcome --> |No| SplitOne["Split total cost to Team One"]
SplitOne --> Result["Per-team expenses"]
SplitTwo --> Result
```

**Diagram sources**
- [GameExpenseCalculator.java:42-81](file://BadmintonCourtManagement/src/main/java/com/badminton/service/calculator/GameExpenseCalculator.java#L42-L81)
- [ServiceUtil.java:149-152](file://BadmintonCourtManagement/src/main/java/com/badminton/util/ServiceUtil.java#L149-L152)

**Section sources**
- [GameExpenseCalculator.java:1-83](file://BadmintonCourtManagement/src/main/java/com/badminton/service/calculator/GameExpenseCalculator.java#L1-L83)
- [ServiceUtil.java:1-175](file://BadmintonCourtManagement/src/main/java/com/badminton/util/ServiceUtil.java#L1-L175)
- [MoneyUtils.java:1-27](file://BadmintonCourtManagement/src/main/java/com/badminton/util/MoneyUtils.java#L1-L27)

### Payment Method Tracking, Timestamp Recording, and Session-Based Consolidation
- Payment method tracking:
  - PayServiceImpl sets payType from PayRequest onto AvailablePlayer.
- Timestamp recording:
  - Leave time is set to UTC+7 Instant via SessionServiceImpl.getUTCPlus7Instant.
- Session-based consolidation:
  - Current session is resolved via SessionServiceImpl.findListCurrentSession.
  - Closing sessions and removing players are supported for end-of-day consolidation.

```mermaid
sequenceDiagram
participant PSI as "PayServiceImpl"
participant SS as "SessionServiceImpl"
participant APR as "AvailablePlayerRepository"
participant AP as "AvailablePlayer"
PSI->>SS : "getUTCPlus7Instant()"
SS-->>PSI : "Instant"
PSI->>APR : "save(AvailablePlayer with payType, payAmount, leaveTime)"
APR-->>PSI : "Persisted"
```

**Diagram sources**
- [PayServiceImpl.java:64-69](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L64-L69)
- [SessionServiceImpl.java:63-70](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L63-L70)
- [AvailablePlayerRepository.java:17-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L17-L34)

**Section sources**
- [PayServiceImpl.java:54-79](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L54-L79)
- [SessionServiceImpl.java:63-70](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L63-L70)
- [AvailablePlayer.java:30-37](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L30-L37)

### MoneyUtils and Amount Formatting
- MoneyUtils.formatToVND formats numeric amounts using Vietnamese locale with dot thousand separators.
- Currency code constant is available for consistency.
- Frontend MoneyUtils.formatVND provides enhanced formatting with improved string handling for values with dot thousand separators.

**Updated** The frontend MoneyUtils now includes improved string cleaning for values with dot thousand separators (e.g., "15.000") before parsing, ensuring accurate currency formatting across different input formats.

**Section sources**
- [MoneyUtils.java:10-25](file://BadmintonCourtManagement/src/main/java/com/badminton/util/MoneyUtils.java#L10-L25)
- [MoneyUtils.js:3-17](file://bad-court-mana-ui/src/page/MoneyUtils.js#L3-L17)

### PayConfirm Dialog: Redesigned Banking-Style UI
- Modern banking-style interface with structured layout replacing raw dialog text.
- Colored header banner with circular icon (green checkmark for payment, red cancel for cancellation).
- Hero amount display with prominent VND currency label.
- Player name display with PersonOutlinedIcon.
- Receipt-style service breakdown with individual costs.
- Bold "Tổng cộng" total footer.
- Two full-width, rounded action buttons ("Huỷ" / "Xác nhận thanh toán" or "Xác nhận huỷ").
- MUI v9 icon compatibility with CheckCircleIcon and PersonOutlinedIcon.

**New** The PayConfirm dialog provides an enhanced user experience with a professional banking-style interface that improves clarity and reduces user confusion during payment confirmation workflows.

**Section sources**
- [PayConfirm.js:19-210](file://bad-court-mana-ui/src/page/dialog/PayConfirm.js#L19-L210)
- [HomePage.js:705-737](file://bad-court-mana-ui/src/page/HomePage.js#L705-L737)

### Service Integration Patterns and JSON Serialization
- ServiceUtil.buildJsonArrayStr converts a list of service DTOs into a JSON string.
- ServiceUtil.addServiceToJsonArray appends a new service DTO to an existing JSON array string.
- ServiceUtil.divideServiceFromJsonArray filters out a specific service DTO from an existing JSON array string.
- These utilities enable dynamic addition/removal of services during a session.

**Section sources**
- [ServiceUtil.java:56-70](file://BadmintonCourtManagement/src/main/java/com/badminton/util/ServiceUtil.java#L56-L70)

### Error Handling for Payment Failures
- Validation errors:
  - IllegalArgumentException mapped to BAD_REQUEST with request validation message.
- Business errors:
  - BusinessException mapped to CONFLICT with business error message.
- Unexpected errors:
  - Other Throwables mapped to INTERNAL_SERVER_ERROR with a generic server error message.
- ServiceTemple centralizes error translation and ensures consistent Result responses.

**Section sources**
- [ServiceTemple.java:23-35](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L23-L35)

### Relationship Between Game Completion, Service Usage, and Automated Billing
- Game completion drives per-game expense calculation via GameExpenseCalculator.
- Services requested by players are aggregated into AvailablePlayer.services as a JSON array.
- At payment time, PayServiceImpl consolidates services and total expense, records payment metadata, and persists the record for billing reconciliation.

**Section sources**
- [GameExpenseCalculator.java:42-81](file://BadmintonCourtManagement/src/main/java/com/badminton/service/calculator/GameExpenseCalculator.java#L42-L81)
- [PayServiceImpl.java:61-69](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L61-L69)
- [ServiceUtil.java:56-70](file://BadmintonCourtManagement/src/main/java/com/badminton/util/ServiceUtil.java#L56-L70)

### Typical Payment Scenarios and Financial Reporting Workflows
- Scenario 1: Player pays for services used during the current session
  - Player selects services; PayRequest includes serviceRequests and totalExpense.
  - PayServiceImpl serializes services, records payType, payAmount, and leaveTime.
  - MoneyUtils can format amounts for display/reporting.
  - Enhanced PayConfirm dialog provides clear confirmation interface.
- Scenario 2: End-of-day session closure
  - SessionServiceImpl closes inactive sessions, cancels in-progress games, and removes players who did not check out.
  - Billing records consolidate payments and services for the day.
- Scenario 3: Revenue tracking
  - AvailablePlayer records capture payType and payAmount per player.
  - ServiceUtil JSON arrays enable detailed breakdowns of services consumed.
  - Frontend displays enhanced currency formatting for financial reports.

**Updated** The redesigned PayConfirm dialog significantly improves the user experience for payment confirmation workflows, providing clearer visual cues and professional banking-style presentation that enhances trust and reduces errors.

### Enhanced User Experience Features
- Professional banking-style UI with consistent color schemes and typography.
- Clear visual hierarchy with prominent amount display and service breakdown.
- Intuitive action buttons with appropriate colors (green for payment, red for cancellation).
- Responsive design with full-width buttons and proper spacing.
- Iconography that reinforces the payment/cancellation actions.
- Improved accessibility with proper contrast ratios and readable fonts.

**Section sources**
- [PayConfirm.js:47-204](file://bad-court-mana-ui/src/page/dialog/PayConfirm.js#L47-L204)
- [HomePage.js:705-737](file://bad-court-mana-ui/src/page/HomePage.js#L705-L737)

## Dependency Analysis
The following diagram highlights key dependencies among payment and financial components:

```mermaid
graph LR
PC["PaymentController"] --> PS["PayService"]
PS --> PSI["PayServiceImpl"]
PSI --> ST["ServiceTemple"]
PSI --> APR["AvailablePlayerRepository"]
PSI --> SS["SessionServiceImpl"]
PSI --> SU["ServiceUtil"]
APR --> AP["AvailablePlayer"]
SS --> APR
GEC["GameExpenseCalculator"] --> SU
MU["MoneyUtils"] --> PSI
MUF["MoneyUtils (Frontend)"] --> PCF["PayConfirm"]
HC["HomePage"] --> PCF
PCF --> MU
```

**Diagram sources**
- [PaymentController.java:1-29](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/PaymentController.java#L1-L29)
- [PayService.java:1-12](file://BadmintonCourtManagement/src/main/java/com/badminton/service/PayService.java#L1-L12)
- [PayServiceImpl.java:1-81](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L1-L81)
- [ServiceTemple.java:1-44](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L1-L44)
- [AvailablePlayerRepository.java:1-35](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L1-L35)
- [AvailablePlayer.java:1-58](file://BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java#L1-L58)
- [SessionServiceImpl.java:1-322](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L1-L322)
- [GameExpenseCalculator.java:1-83](file://BadmintonCourtManagement/src/main/java/com/badminton/service/calculator/GameExpenseCalculator.java#L1-L83)
- [MoneyUtils.java:1-27](file://BadmintonCourtManagement/src/main/java/com/badminton/util/MoneyUtils.java#L1-L27)
- [ServiceUtil.java:1-175](file://BadmintonCourtManagement/src/main/java/com/badminton/util/ServiceUtil.java#L1-L175)
- [PayConfirm.js:1-210](file://bad-court-mana-ui/src/page/dialog/PayConfirm.js#L1-L210)
- [MoneyUtils.js:1-21](file://bad-court-mana-ui/src/page/MoneyUtils.js#L1-L21)
- [HomePage.js:1-932](file://bad-court-mana-ui/src/page/HomePage.js#L1-L932)

**Section sources**
- [PayServiceImpl.java:28-81](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L28-L81)
- [AvailablePlayerRepository.java:15-34](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L15-L34)
- [SessionServiceImpl.java:140-144](file://BadmintonCourtManagement/src/main/java/com/badminton/service/SessionServiceImpl.java#L140-L144)

## Performance Considerations
- Transaction boundaries:
  - PayServiceImpl is annotated with @Transactional to ensure atomicity of payment persistence.
- Concurrency:
  - AvailablePlayerRepository uses pessimistic locking hints for reads and writes to prevent race conditions when checking out players.
- Timezone handling:
  - UTC+7 Instant is consistently used for timestamps to align with database expectations and local operations.
- JSON serialization:
  - ServiceUtil uses Gson for efficient serialization/deserialization of service lists.
- Frontend performance:
  - PayConfirm dialog uses memoization and efficient rendering with Material UI components.
  - Enhanced currency formatting uses browser's Intl.NumberFormat for optimal performance.

**Updated** The redesigned PayConfirm dialog maintains excellent performance through efficient Material UI component usage and optimized rendering strategies.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
- Request validation failures:
  - Ensure playerName, serviceRequests, totalExpense, and payType are present and non-blank.
  - IllegalArgumentException will be translated to BAD_REQUEST with a validation message.
- Player not found:
  - Verify the player exists in the current session and has not checked out (leaveTime is null).
- Business rule violations:
  - BusinessException will be translated to CONFLICT with a business-specific message.
- Internal errors:
  - Unexpected exceptions are captured and returned as INTERNAL_SERVER_ERROR with a generic message.
- Frontend currency formatting issues:
  - Ensure values are properly cleaned of thousand separators before parsing.
  - Verify locale support for "vi-VN" formatting.

**Updated** The PayConfirm dialog provides clear error feedback through its visual design, with appropriate colors and icons indicating success or failure states.

**Section sources**
- [PayServiceImpl.java:46-51](file://BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java#L46-L51)
- [AvailablePlayerRepository.java:17-19](file://BadmintonCourtManagement/src/main/java/com/badminton/repository/AvailablePlayerRepository.java#L17-L19)
- [ServiceTemple.java:23-35](file://BadmintonCourtManagement/src/main/java/com/badminton/service/ServiceTemple.java#L23-L35)
- [MoneyUtils.js:3-17](file://bad-court-mana-ui/src/page/MoneyUtils.js#L3-L17)

## Conclusion
The Payment and Financial System provides a robust, transactionally wrapped pipeline for processing payments, aggregating services, and persisting billing metadata. ServiceTemple ensures consistent error handling, SessionServiceImpl provides accurate time context, and ServiceUtil enables flexible service management. MoneyUtils supports VND formatting for financial reporting. The redesigned PayConfirm dialog with banking-style UI significantly enhances the user experience for payment confirmation workflows, providing clear visual cues and professional presentation. Together, these components deliver automated billing, reliable payment completion, and session-based revenue consolidation with improved user interface design.

**Updated** The system now combines backend reliability with frontend excellence, offering both robust payment processing capabilities and an intuitive, professional user interface that enhances the overall payment experience.

## Appendices
- Payment endpoint: POST /api/v1/pay/payToPlayer
- Payment types: PAY, CANCEL (PayType enum)
- Currency: VND (MoneyUtils.formatToVND, MoneyUtils.formatVND)
- Service requests: ServiceRequest with serviceName and cost
- Response payload: PayResponse with playerName, services, payType, payAmount, payTime
- PayConfirm dialog: Enhanced banking-style UI with colored headers, circular icons, and receipt-style service breakdowns
- Frontend integration: HomePage manages PayConfirm dialog state and handles payment confirmation events

[No sources needed since this section summarizes previously cited components]