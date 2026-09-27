# Report Data Loading Analysis

Analysis of data loaded in `ExcelExportService` (`com.badminton.service.impl.ExcelExportService`).

---

## 1. `reportList(ReportListRequest)` — line 122

Paginated list of session report summaries.

### Input
`ReportListRequest`:
- `yearMonth` — filter period (e.g. `"2026-09"`), converted via `TimeUtils.convertYearMonthToInstant` into a `from`/`to` Instant range.
- `date`, `fromToDate` — present in the request model but **not used** by this flow.
- `pagination` — `current` page index and `pageSize` (normalized in `validatePagination`).

### Data loaded (queries)
| Source | Call | Data |
|---|---|---|
| `SessionRepository` | `sessionService.countSessionBy(yearMonth, pagination)` → `countByFromTimeBetween(from, to)` | Total count of sessions in the month range |
| `SessionRepository` | `sessionService.findListSessionBy(yearMonth, pagination)` → `findByFromTimeBetween(from, to, pageable)` | Page of `Session` entities, sorted `fromTime` DESC |
| (lazy) | `Session.getAvailablePlayers()` per session | `AvailablePlayer` list — loaded lazily per session (**potential N+1**, one extra query per session) |

### Fields consumed per `Session`
- `sessionId` → `ReportResponse.sessionId`
- `fromTime` → `ReportResponse.date`
- `fromTime` + `toTime` → `ReportResponse.during` (`TimeUtils.convertInstantsToString`)
- `availablePlayers[].payAmount` → summed into `ReportResponse.grossRevenue` / `grossRevenueFormat` (only players with non-null `payAmount`)

### Output
`PageResponse<ReportResponse>`: `{ total, list[ReportResponse(no, sessionId, date, during, grossRevenue, grossRevenueFormat)], pagination(totalPage) }`.

> Note: `totalPage` is computed as `count / pageSize + 1`, which over-counts by 1 when `count % pageSize == 0`.

---

## 2. `exportReport(String sessionId)` — line 158

Generates a single-session `.xlsx` report. Runs inside `@Transactional`, so lazy associations stay resolvable.

### Input
- `sessionId` — string session id, asserted non-blank.

### Data loaded — `retrieveReport(sessionId)` (line 689)
| Source | Call | Data loaded |
|---|---|---|
| `SessionRepository` | `sessionService.findSessionById(sessionId)` | `Session` entity (uses `fromTime`, `toTime`) |
| `AvailablePlayerRepository` | `findAllBySession(session)` | All `AvailablePlayer` of the session — **PESSIMISTIC_READ locked**; each holds `player`, `payAmount`, `leaveTime`, `payType`, `services` / `currentServices` (JSON string) |
| `GameRepository` | `findGamesByPlayerIds(avaIds)` | `Game` entities where any `teamOne`/`teamTwo` player `avaId` matches — `JOIN FETCH court`, `LEFT JOIN FETCH teamOne`, `LEFT JOIN FETCH teamTwo`. Lazily resolved during writing: `shuttleMap` (`GameShuttleMap` → `ShuttleBall`), team `playerOne`/`playerTwo` |
| `RentByTimeRepository` | `findByAvailablePlayerAvaId(avaId)` per player | `RentByTime` records per available player (**N+1**: one query per player); only `shuttles` JSON column is consumed |

### Data derived — `buildReportModel(...)` → `ReportDTO`
- **`listTotalShuttle`** (`List<RptShuttle>`) — aggregated shuttle quantity per shuttle name:
  - from `Game.shuttleMap`: `ShuttleBall.shuttleName`, `cost`, `GameShuttleMap.shuttleNumber`
  - plus `RentByTime.shuttles` JSON → `RentShuttleDTO`(`shuttleName`, `cost`, `number`)
- **`listTotalService`** (`List<RptService>`) — per-service quantity count aggregated across all players' `currentServices` JSON (`ServiceDTO`), **excluding**:
  - court fee per person (`GameConstant.COST_IN_PERSON_VN`)
  - advance payment (`GameConstant.ADVANCE_PAYMENT_VN`)
  - rent-by-time services (`RentConstant.RENT_BY_TIME_STR`, `RENT_BY_TIME_EN_STR`)
- `listCost` is passed as `null` (the `ReportCost`-based column writer is commented out).

### Fields consumed when writing the workbook
- `Session.fromTime`, `Session.toTime` — title, `Ngày`, `Ngày đinh dạng`, `Thời gian` header cells and file name (`report_yyyyMMdd_fromh-toh.xlsx`)
- `AvailablePlayer` — `avaId` (partner map), `player.playerName`, `payAmount` (row value + `Tổng tiền` sum), `leaveTime`, `services` JSON → court fee cell + remaining-service name/cost column pairs
- `Game` — `court.courtName`, `createdDate`, `endedDate`, `teamOne`/`teamTwo` (`playerOne`/`playerTwo`, `expenseOne`/`expenseTwo`, `isWin`) → game title + partner name + expense cells
- `RptShuttle` / `RptService` — `Tổng cầu <name>` / `Tổng <name>` header + quantity cells

### Output
`ExportReportResult` — `ByteArrayResource` of the generated xlsx + computed file name.

---

## 3. Shared entity data summary

| Entity | Fields used by reports |
|---|---|
| `Session` | `sessionId`, `fromTime`, `toTime`, `availablePlayers` |
| `AvailablePlayer` | `avaId`, `player` (`playerName`), `payAmount`, `leaveTime`, `payType`, `services`/`currentServices` JSON |
| `Game` | `court` (`courtName`), `createdDate`, `endedDate`, `teamOne`, `teamTwo`, `shuttleMap` |
| `Team` | `playerOne`, `playerTwo`, `expenseOne`, `expenseTwo`, `win` |
| `GameShuttleMap` | `shuttleBall` (`shuttleName`, `cost`), `shuttleNumber` |
| `RentByTime` | `shuttles` JSON (`RentShuttleDTO`: `shuttleName`, `cost`, `number`) |

## 4. Query / performance notes
- `reportList` loads `availablePlayers` lazily per session → N+1 risk proportional to page size.
- `exportReport` → `retrieveReport` issues one `findByAvailablePlayerAvaId` per player → N+1; could be replaced by a single `findByAvailablePlayerAvaIdIn(avaIds)`.
- `Game.shuttleMap` is not fetch-joined in `findGamesByPlayerIds` → lazily loaded per game inside the transaction.
- `findAllBySession` uses `PESSIMISTIC_READ` lock — rows are locked while the report is generated.
