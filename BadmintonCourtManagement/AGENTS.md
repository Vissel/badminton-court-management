# Agent Guidelines for BadmintonCourtManagement

This file captures project-specific conventions, feature contracts, and design decisions for the BadmintonCourtManagement backend.

## Project Stack

- Java 21 (see `JDK21_SETUP.md` and `.sdkmanrc`)
- Maven (`pom.xml`, `mvnw`)
- Spring Boot with Spring Data JPA

## Design Conventions

- **Transactional consistency**: Payment and allocation flows (debit, rent, session mutations) should be fully transactional and roll back on any failure, while still surfacing user-facing message details to the calling service layer (e.g., `DebitServiceImpl`).
- Avoid importing, exporting, or deleting system-level product rows such as `costInPerson` and `rentByTime` (`ApiConstant`).
- Product updates that affect historical linkage should deactivate the old record and insert a new one to preserve history (`GameShuttleMap` / service references).

## Product Import / Export

Implementation lives in `com.badminton.service.product` and `com.badminton.service.report`.

- **Controller**: `ProductsController` under `/api/products/**`
- **Endpoints**:
  - `GET /api/products/export` — returns `.xlsx` with two sheets: `ShuttleBall` and `Service`. Columns are `Tên` and `Giá`.
  - `GET /api/products/template` — returns a headers-only `.xlsx` matching the import format.
  - `POST /api/products/import/preview` — accepts a multipart file and optional `mode` (`REPLACE`). Returns an `importToken`, per-row actions (`ADD`, `UPDATE`, `REACTIVATE`, `SKIP`, `ERROR`), and counts. Performs no DB writes.
  - `POST /api/products/import/commit` — accepts the `importToken` and applies the cached plan transactionally.
- **Core classes**:
  - `ProductImportExportServiceImpl` — caches preview plans in a `ConcurrentHashMap` with a 30-minute TTL; tokens are single-use.
  - `ProductExcelParser` — parses uploaded workbooks.
  - `ProductImportApplier` — applies the classified import plan.
  - `ProductExcelWriter` — generates exports and templates.
- **REPLACE mode**: deactivates active products that do not appear in the uploaded file.
- **Authorization**: all endpoints require `ROLE_ROOT` or `ROLE_ADMINISTRATOR`; otherwise return `403 Forbidden`. Authorization comes from JWT authorities, not a hard-coded username.
- **File size**: multipart uploads are capped at 5 MB in `application.properties`.
- **Tests**: `ProductImportExportServiceImplTest` covers the flow.

## Inventory Management

Stock is tracked via an append-only ledger — `stock_movement` — never as a mutable
counter. Design detail: `inventory-redesign.md`; plan/DoD: `plan2026.md` feature #10.

- **Controller**: `InventoryController` under `/api/inventory/**` — management endpoints require `ROLE_ROOT` or `ROLE_ADMINISTRATOR`; `/checkStock` also permits `ROLE_COORDINATOR` for HomePage workflows.
- **Endpoints**: `GET/POST/PUT /api/inventory/items`, `POST /api/inventory/purchases`, `GET /api/inventory/movements`, `POST /api/inventory/adjustments`, `GET /api/inventory/export`, `POST /api/inventory/import/preview|commit` (StockIntake sheet: `Tên | Số lượng | Giá nhập | Ngày nhập | Đơn vị`, token flow identical to product import).
- **Entities**: `InventoryItem` (stable stock identity), `PurchaseLot` (wholesale intake), `StockMovement` (ledger: `PURCHASE_IN`, `GAME_CONSUMPTION`, `RETAIL_SALE`, `ADJUSTMENT`, `RETURN`).
- **Units**: the ledger always counts in the item's base unit (`unit`, e.g. `quả`). Optional `packageUnit` + `unitsPerPackage` (e.g. `ống` × 12, auto-defaulted for `SHUTTLE_BALL`) drive intake conversion — `POST /purchases` and the StockIntake `Đơn vị` column accept `PACKAGE`/`BASE`/literal labels; blank defaults to the package unit. `purchase_lot` stores the bought `unit`/`quantity` plus `base_quantity`; `stock_movement.quantity_delta`/`unit_cost` are always base units.
- **Linkage**: `shuttle_ball.item_id` and `service.item_id` point catalog rows to an `InventoryItem`. Stock attaches to the item so it survives the deactivate+insert price-history convention. Non-stockable services (`costInPerson`, `rentByTime`) have no item.
- **Stock math**: on hand = `SUM(quantity_delta)`; margin = `retailPrice − weightedAvgCost` (moving average replayed over the ledger); low stock = ≤ 10.
- **Deduction hooks**: `CourtServicesService.changeGameState` writes `GAME_CONSUMPTION` on game FINISH (non-blocking) and a low-stock warning on START; service retail flow uses Option A — `addServiceToAvailablePlayer` writes `RETAIL_SALE` immediately, `removeServiceOutAvailablePlayer`/`updateServicesToAvailablePlayer` (net delta) write `RETURN`, and `SessionServiceImpl.removeListPlayerOutCurrentSession` / `closeOutDateSession` return all unbilled services. `CorePaymentService.payForPlayerAndCreateDebt` only finalizes billing and does **not** touch stock.
- **Catalog stock exposure**: `ServiceResponse` and `ShuttleBallResponse` carry `itemId`, `stockOnHand` (base units), and `lowStock`; computed from the linked `InventoryItem`.
- **DTOs**: `ServiceRequest`/`ServiceDTO` carry optional `itemId` + `quantity`; absent fields parse as legacy non-stockable lines.

## Authentication and RBAC

- Staff accounts are stored in `app_user`; court players remain in `player` and cannot log in.
- Roles are `ROOT`, `ADMINISTRATOR`, `COORDINATOR`, and `PLAYER`, linked through `app_user_role`.
- Authentication is stateless: RS256 JWT access tokens plus opaque, SHA-256-hashed, rotating refresh tokens.
- `ROOT` has all permissions; `ADMINISTRATOR` has all except `/admin/internal/**`; `COORDINATOR` is limited to HomePage API workflows; `PLAYER` has no application access.
- Player names are unique. Migration `rbac-jwt.sql` rewires `available_player`, `debit`, `debit_summary`, and `payment` to each duplicate name's minimum `player_id` before deleting duplicates and adding `uk_player_name`.

## Debit Prepay Preview

- Endpoint: `POST /api/v1/debit/prePay` in `DebitController`.
- Service method: `DebitService.prepayDebitsForPlayer(PayDebitRequest)`.
- Response: `PrepayDebitResponse` with `playerName`, `paymentAmount`, and a list of `PrepayDebit` entries.
- Each `PrepayDebit` entry contains a `RemainingDebitsResponse` (`payDebit`) and a `payStatus` derived from the `PrepayStatus` enum (`PARTIALLY_PAID`, `FULL_PAY`).
- The prepay endpoint is a read-only allocation preview and must not write to the database.
