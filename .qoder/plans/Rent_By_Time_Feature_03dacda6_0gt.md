# Rent By Time Implementation Plan

## 1. Database
**File:** `BadmintonCourtManagement/src/main/resources/db/changelog/badminton-qa.sql`
Add `rent_by_time` table: `id`, `ava_id` (FK), `court_id` (FK), `start_time`, `end_time`, `num_time` decimal(4,2), `shuttles` varchar(500), `state` varchar(50). Values: `Started`, `Finish`, `Cancel`.

## 2. Backend Entity & Repository
**Files:**
- `entity/RentByTime.java` — JPA entity with `Instant` timestamps, `BigDecimal numTime`, `String shuttles`.
- `repository/RentByTimeRepository.java` — query by `courtId + state`, `avaId`, `state`.

## 3. Backend DTOs / Utilities
**Files:**
- `model/dto/RentShuttleDTO.java` — `shuttleId`, `shuttleName`, `cost`, `number`.
- `requestmodel/RentByTimeRequest.java` — `courtId`, `playerName`, `numTime`, `shuttleBalls`.
- `response/RentByTimeResponse.java` — rental fields + `remainingMinutes`.
- Add Gson JSON converter for `shuttles` in `ServiceUtil` or a new utility class.

## 4. Backend Service
**File:** `service/RentByTimeService.java`
- `applyRentByTime(request)` — validate one player on court, compute `endTime` from DB time + `numTime`, persist rental as `Started`, add `"rentByTime"` service to player `services` JSON with cost = `numTime * 100000`.
- `payRentByTime(rentId)` — set `Finish`, snap `endTime` if needed.
- `cancelRentByTime(rentId)` — set `Cancel`, remove `rentByTime` service from player.
- `updateRentByTime(rentId, request)` — extend `numTime` / recalc `endTime`, update shuttles & cost.
- `getActiveRentByTimeForCourt(courtId)` — return active rental or null.
- `getCurrentDbTime()` — wrap `sessionService.getUTCPlus7Instant()` for frontend sync.

## 5. Backend Controller
**File:** `controller/CourtManagementController.java`
Add under `/court-mana`:
- `POST /applyRentByTime`
- `POST /payRentByTime?rentId=`
- `POST /cancelRentByTime?rentId=`
- `POST /updateRentByTime?rentId=`
- `GET /getActiveRentByTime?courtId=`
- `GET /getCurrentTime`

## 6. Frontend Court Component
**File:** `page/dragNdrop/Court.js`
- Replace `"Bắt đầu"` button with a split button: main click starts normal game; dropdown arrow opens a small `Menu` with option `"Thuê theo giờ"`.
- When active rental exists, treat as `isLocked`: show countdown in header text, display `Kết thúc` / `Huỷ` buttons.
- Pass new props: `onRentByTime(courtId)`, `rentalInfo`.
- Countdown uses frontend `setInterval` synced to DB time from `/getCurrentTime`.

## 7. Frontend RentByTimeDialog
**New file:** `page/dialog/RentByTimeDialog.js`
Fields: court name (read-only), start time (from DB), number of hours input (default 1, step 0.5), end time (auto-calculated), fee (auto-calculated), shuttle ball list with quantity inputs. Bottom-right `"Bắt đầu"` button triggers `onConfirm`. Supports an `editMode` prop for admin updates.

## 8. Frontend HomePage Integration
**File:** `page/HomePage.js`
- State: `rentals` map `courtId -> rental object`.
- `onRentByTime(courtId)` — open `RentByTimeDialog` pre-filled with court and current DB time.
- `confirmRentByTime(data)` — call `applyRentByTime`, on success lock court, start countdown, add `"Thuê theo giờ"` service to `playerServiceMap`.
- `onFinishRent(courtId)` / `onCancelRent(courtId)` — call pay/cancel APIs, unlock court, remove service from `playerServiceMap`.
- On initial load (`useEffect`), fetch active rentals via `getActiveRentByTime` and populate state.
- Pass `rentalInfo` and handlers down to `Court`.

## 9. Frontend ServiceDialog Update
**File:** `page/dialog/ServiceDialog.js`
- If a service name contains `"rentByTime"`, render display text as `"Thuê theo giờ <courtName>"` instead of the raw key.

## 10. Excel Export Update
**File:** `service/impl/ExcelExportService.java`
- In `retrieveReport`, also fetch `RentByTime` records for the session via `RentByTimeRepository` (join through `AvailablePlayer`).
- Include rental shuttles in total shuttle accumulation.
- Include rental-derived services in the player's service list so they appear in rows and totals.
- In `buildListTotalService`, ensure `rentByTime` services are counted correctly.

## 11. Testing / Verification Checklist
- One player on court → dropdown shows → dialog opens → start rental → court locks, countdown runs.
- Drag/drop another player to same court is blocked during rental.
- ServiceDialog shows `"Thuê theo giờ Sân X"`.
- Admin can update time/shuttles.
- Finish rental clears lock and adds final service; cancel removes it.
- Export report includes rental data and shuttles in totals.