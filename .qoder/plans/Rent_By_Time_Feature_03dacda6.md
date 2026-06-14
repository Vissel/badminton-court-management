# Rent By Time (Thuê theo giờ) Implementation Plan

## Overview
Add a timed court-rental mode where one player can rent a court by the hour. During rental, the court is locked and a countdown is shown. Admin can extend time or update shuttle balls. The feature spans DB schema, backend APIs, frontend UI/dialogs, and Excel export.

---

## Task 1: Database Schema

**File:** `BadmintonCourtManagement/src/main/resources/db/changelog/badminton-qa.sql`

Add a new `rent_by_time` table:
```sql
CREATE TABLE `rent_by_time`
(
    `id`         int NOT NULL AUTO_INCREMENT,
    `ava_id`     bigint NOT NULL,
    `court_id`   int NOT NULL,
    `start_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    `end_time`   timestamp NULL DEFAULT NULL,
    `num_time`   decimal(4,2) DEFAULT '1.00',
    `shuttles`   varchar(500) DEFAULT NULL,
    `state`      varchar(50) DEFAULT 'Started',
    PRIMARY KEY (`id`),
    KEY `rent_ava_fk` (`ava_id`),
    KEY `rent_court_fk` (`court_id`),
    CONSTRAINT `rent_ava_fk` FOREIGN KEY (`ava_id`) REFERENCES `available_player` (`ava_id`),
    CONSTRAINT `rent_court_fk` FOREIGN KEY (`court_id`) REFERENCES `court` (`court_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

Notes:
- `num_time` uses `decimal(4,2)`: `1.00` = 1 hour, `1.50` = 1h 30m, `0.50` = 30m.
- `shuttles` stores a JSON array string of shuttle objects (`shuttleId`, `shuttleName`, `cost`, `number`).
- `state` values: `Started`, `Finish`, `Cancel`.

---

## Task 2: Backend Entity

**File:** `BadmintonCourtManagement/src/main/java/com/badminton/entity/RentByTime.java`

- JPA entity mapped to `rent_by_time`.
- Fields: `id`, `availablePlayer` (`@ManyToOne` to `AvailablePlayer`), `court` (`@ManyToOne` to `Court`), `startTime`, `endTime`, `numTime` (`BigDecimal`), `shuttles` (`String`), `state` (`String`).
- Use `Instant` for timestamps (consistent with existing entities).

---

## Task 3: Backend Repository

**File:** `BadmintonCourtManagement/src/main/java/com/badminton/repository/RentByTimeRepository.java`

- `Optional<RentByTime> findByCourtCourtIdAndState(int courtId, String state)`
- `List<RentByTime> findByAvailablePlayerAvaId(long avaId)`
- `List<RentByTime> findByCourtCourtId(int courtId)`
- `List<RentByTime> findByState(String state)`

---

## Task 4: Backend DTOs / Request Models

**Files:**
- `BadmintonCourtManagement/src/main/java/com/badminton/model/dto/RentShuttleDTO.java` — maps one shuttle entry for JSON: `shuttleId`, `shuttleName`, `cost`, `number`.
- `BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/RentByTimeRequest.java` — request body for apply/update: `courtId`, `playerName`, `numTime` (float), `shuttleBalls` (`List<ShuttleBallDTO>`).
- `BadmintonCourtManagement/src/main/java/com/badminton/response/RentByTimeResponse.java` — response for frontend: `id`, `courtName`, `playerName`, `startTime`, `endTime`, `numTime`, `shuttleBalls`, `state`, `remainingMinutes`.

**Utility:** Add Gson-based converter methods in `ServiceUtil` (or a new `RentByTimeUtil`) to serialize/deserialize `shuttles` JSON string to/from `List<RentShuttleDTO>`.

---

## Task 5: Backend Service Layer

**File:** `BadmintonCourtManagement/src/main/java/com/badminton/service/RentByTimeService.java`

Methods (all `@Transactional`):
1. `applyRentByTime(RentByTimeRequest request)`
   - Validate court has exactly one player placed.
   - Compute `endTime = startTime + numTime hours` using DB current time (`sessionService.getUTCPlus7Instant()`).
   - Persist `RentByTime` with state `Started`.
   - Add service `"Thuê theo giờ <courtName>"` (service key `rentByTime`) to the player's `services` JSON via `ServiceUtil.addServiceToJsonArray` with cost = `numTime * 100000` (default hourly rate).
   - Return `RentByTimeResponse`.

2. `payRentByTime(int rentId)`
   - Find rental, set state = `Finish`, set `endTime` = current DB time if not already passed.
   - Return the player's updated service list or a boolean.

3. `cancelRentByTime(int rentId)`
   - Set state = `Cancel`, set `endTime` = current DB time.
   - Remove the `rentByTime` service from the player's `services` JSON.

4. `updateRentByTime(int rentId, RentByTimeRequest request)`
   - Allow extending `numTime` (recalculate `endTime`) and updating `shuttles`.
   - Update the player's service cost if `numTime` changes.

5. `getActiveRentByTimeForCourt(int courtId)` — returns active rental or null.

6. `getCurrentDbTime()` — returns `sessionService.getUTCPlus7Instant()` as a simple `Instant` wrapper for the frontend countdown sync.

---

## Task 6: Backend Controller Endpoints

**File:** `BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java`

Add endpoints under existing `/court-mana` mapping:
- `POST /applyRentByTime` → calls `rentByTimeService.applyRentByTime(...)`
- `POST /payRentByTime?rentId={rentId}` → calls `payRentByTime(...)`
- `POST /cancelRentByTime?rentId={rentId}` → calls `cancelRentByTime(...)`
- `POST /updateRentByTime?rentId={rentId}` → calls `updateRentByTime(...)`
- `GET /getActiveRentByTime?courtId={courtId}` → returns active rental JSON or empty.
- `GET /getCurrentTime` → returns current DB time (`Instant`) so frontend can sync countdown accurately.

---

## Task 7: Frontend Court Component — Split Button Dropdown

**File:** `bad-court-mana-ui/src/page/dragNdrop/Court.js`

- Replace the plain `Button` for `"Bắt đầu"` with a split-button pattern:
  - Main button text: `"Bắt đầu"` (starts a normal game, existing behavior).
  - Small dropdown arrow on the right opens a `Menu` with one extra option: `"Thuê theo giờ"`.
- When a player is in the court and rental is active, show countdown timer in the court header (e.g., `"Sân 1 (Còn 23:45)"`).
- Pass new props from `HomePage`: `onRentByTime`, `rentalInfo`.
- Keep `isLocked` behavior: if rental is active, treat court as locked (no drag/drop, show countdown + finish/cancel buttons).

---

## Task 8: Frontend RentByTimeDialog

**New file:** `bad-court-mana-ui/src/page/dialog/RentByTimeDialog.js`

UI fields:
- Court name (read-only)
- Start time (read-only, from DB current time API)
- Number of hours input (default `1.00`, step `0.5`, min `0.5`)
- End time (calculated live: `start + numTime`)
- Fee display (calculated live: `numTime * 100000`, formatted VND)
- Shuttle ball selection: multi-select / list of existing shuttle types with quantity input (no default quantity; user must enter; default can be 0)
- Bottom-right `