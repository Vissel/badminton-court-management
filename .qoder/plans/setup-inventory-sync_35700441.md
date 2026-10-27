# Setup ↔ Inventory sync

## Summary
Today `service` / `shuttle_ball` (Setup page) and `inventory_item` (Quản lý kho) are two catalogs joined only by a nullable `item_id` and a by-name self-heal on read. This plan makes `inventory_item` the source of truth for name, stock, Giá bán and active flag; `service.cost` / `shuttle_ball.cost` stay the billing price and are kept mirrored. Setup becomes a product catalog view of inventory instead of an independent price list.

## Decisions taken (change any of these before implementation starts)
1. **Price**: one number, mirrored. `inventory_item.retail_price` is master; saving it pushes `service.cost` / `shuttle_ball.cost` (billing keeps reading `cost`, so no billing/HomePage change). Saving a price on the Setup page pushes it back into `retail_price`.
2. **Lifecycle**: deleting (or deactivating) on Setup deactivates both the catalog row and the inventory item; restoring in either place reactivates both. Stock ledger history is never deleted.
3. **Images: dropped from scope.** No `image_url` column, no upload endpoint, no thumbnail on either page. A catalog row is identified by its name plus the category icon that already exists (`ItemTypes.js`). Do not add image fields to the entities, requests/responses or dialogs.
4. **Zero stock must not block selling** (synced items start at 0). Picker keeps the item draggable but shows `Hết hàng`; on drop, a confirm ("Hết hàng, vẫn bán?") is required and stock may go negative until intake. Alternative (block) is a one-line change if preferred.
5. Protected pricing rows `costInPerson`, `rentByTime` never get an inventory item.

## Data model & migration
- No schema change at all: `service.item_id` / `shuttle_ball.item_id` already exist and carry the link.
- Latest applied changeset is `030` (`service-item-relink.sql`); this plan adds no new changeset, so `db.changelog-master.xml` is untouched.

## Backend

### New `com.badminton.core.inventory.CoreCatalogSyncService`
Single choke point for catalog ↔ item sync (replaces the ad-hoc `linkInventoryItem` in `CourtServicesService` and duplicates in `InventoryServiceImpl` / `CoreInventoryService`):
- `InventoryItem ensureItem(String name, InventoryItemType type, BigDecimal retailPrice, boolean active)` — find by (name, type) active-or-inactive, create when missing (unit defaults: `SHUTTLE_BALL` → quả / ống ×12; `GOODS` → no package config), apply price + active, skip protected names.
- `void linkService(Service s, BigDecimal price)` / `void linkShuttleBall(ShuttleBall b, BigDecimal price)` — `ensureItem` + `setItem(...)` + `save`.
- `void syncCatalogFromItem(InventoryItem item)` — push `retail_price` / `is_active` / name into the **active** catalog row(s) with `item_id = item.itemId`, updated **in place** (no deactivate+insert: the catalog row is no longer a price history, the item is). Inactive catalog rows are left untouched so restore keeps working.
- `void deactivateCatalog(InventoryItem item)` — used when Setup deletes.

### Setup endpoints (`SettingsController` → `AdminServiceImpl`)
- `GET /api/getSetupServices` gains `?includeInactive=true` (default `false`, so `ProductPage` is unaffected) and returns every row enriched with item data; rows with the protected names stay mapped to `costInPerson` / `rentByTime` as today.
- `updateServices()` / `updateShuttleBalls()`: every added/reactivated row goes through `linkService` / `linkShuttleBall` with `retailPrice = cost` (this is the sync the user asked for); every deleted row deactivates the catalog row **and** its item via `deactivateCatalog`.
- `setUpService()` (legacy single add) gets the same treatment.
- `SetUpServiceResponse` + `ServiceResponse` / `ShuttleBallResponse` gain additive fields (non-breaking for `ProductPage`, `HomePage`): `unit`, `packageUnit`, `unitsPerPackage`, `retailPrice`, `active`, `stockBreakdown` (e.g. `5 ống + 3 quả`), `itemId`/`stockOnHand`/`lowStock` already exist. No image field.
- `getSetUpService()` must enrich stock itself (today only `CourtServicesService.getActiveServices` and `/court-mana/getShuttleBalls` do) — reuse one `enrichCatalogRow` helper so Setup sees the same numbers as HomePage.

### Inventory endpoints (`InventoryController` / `InventoryServiceImpl`)
- `saveItem()` (POST/PUT `/api/inventory/items`): after persisting, call `syncCatalogFromItem(item)` so Giá bán / active / rename reach the catalog; reject an empty `retailPrice` on create with a clear error (Giá bán becomes mandatory, which is the "missed Giá bán" fix).
- `PUT /api/inventory/items/{id}` keeps its current shape; the Setup page and the inline price editor reuse it (no new price endpoint).
- No upload/resource-serving endpoint is added (image feature dropped).

### Product Excel import
- `ProductImportApplier.saveNew` / `applyReactivate` go through `linkService` / `linkShuttleBall` so imported catalog rows are synced too (otherwise the gap reopens on every import).

## Frontend

### Setup page (`src/page/SetupPage.js`)
- Load with `?includeInactive=true`; both tables become:
  | Tên | Đơn vị | Còn lại | Giá bán | Trạng thái | Thao tác |
- **Còn lại**: `stockOnHand` + `unit`, tooltip with `stockBreakdown`; `—` when the row has no item.
- **Giá bán**: `retailPrice` formatted; when null show an amber `Thiếu giá bán` chip + inline set action (opens the item dialog focused on Giá bán).
- **Trạng thái**: chip `Đang bán` / `Ngừng bán` / `Chưa liên kết kho` (row active vs item active vs `itemId == null`).
- **Thao tác**: edit (reuse `InventoryItemDialog` with `{itemId, itemName, itemType, unit, packageUnit, unitsPerPackage, retailPrice, active}`), open in Quản lý kho (`/inventory`), stock history (reuse `StockLedgerDialog`), delete / restore (existing `isDeleted` toggle → now also deactivates the item).
- Keep the existing add-row forms; reword to "Thêm mặt hàng" and note that saving also creates the inventory item. Duplicate check must ignore inactive rows.
- Add a read-only caption: "Số lượng, giá bán và trạng thái lấy từ Quản lý kho".

### Quản lý kho page (`src/page/InventoryPage.js` + `src/page/dialog/InventoryItemDialog.js`)
- **Inline Giá bán edit**: click the price cell → `TextField` → `PUT /api/inventory/items/{id}` with the full row payload (this is the "add Giá bán to functions" ask); rows with null price get a `Thiếu giá bán` chip and row highlight, plus a "Chỉ hiện thiếu giá bán" filter toggle.
- `InventoryItemDialog`: Giá bán required with helper text "Giá bán này dùng cho trang Cài đặt". No image section.
- After save, the row shows the mirrored `service.cost` on the next Setup load (no extra UI).

### Shared pieces
- HomePage picker (`DraggableService`): keep `canDrag` true when `outOfStock` but style it dimmed with `Hết hàng`; `handleDropService` asks for confirmation before adding an out-of-stock line (decision 4).
- No shared thumbnail component and no image API helper (image feature dropped).

## Edge cases
- Protected rows: never synced (filter by `ApiConstant.COST_IN_PERNSON` / `RENT_BY_TIME`).
- Rename in Quản lý kho: the active catalog row's `ser_name` / `ball_name` is overwritten in place; old player JSON keeps the old name and price (historical bills unchanged).
- Price change from either side: the row is updated in place (`service.cost` ← `retail_price`), one write per change; guard against re-entrancy (Setup save writes item, item save writes catalog — each path is one-way and never calls the other's entry point).
- **Giá bán wins on read**: `getSetUpService` self-heals missing links but always links with `priceOf(catalogCost, item)` = `item.retailPrice` when present, so a stale `service.cost` can never overwrite the inventory price. `link()` only saves when something actually changed.
- Multiple catalog rows for one item (price history): only the active row is updated; `syncCatalogFromItem` filters `isActive`.
- Rows with `itemId == null` (legacy, or no matching item): shown as `Chưa liên kết kho` with a "Tạo trong kho" action that calls `POST /api/inventory/items`; the read-time self-heal then links it.
- `getSetUpService` returns `null` when everything is empty today — keep the null-safety in the FE.

## Rollout order
1. `CoreCatalogSyncService` + rewire `CourtServicesService`, `InventoryServiceImpl`, `CoreInventoryService` to it.
2. Setup read enrichment + `includeInactive`, then `updateServices` / `updateShuttleBalls` / `setUpService` sync, then `ProductImportApplier`.
3. `saveItem` price push + mandatory Giá bán + catalog deactivate on delete.
4. FE: Quản lý kho (inline price, missing-price filter) → Setup columns/actions → HomePage out-of-stock confirm.

## Status: implemented (images excluded, HomePage confirm deferred)
Done: `CoreCatalogSyncService` (+`ServiceItemLinker` ball support), `getSetUpService(includeInactive)` enrichment, Setup add/restore/delete sync, `saveItem` price push with mandatory Giá bán, `ProductImportApplier` rewired, SetupPage columns/status/restore/refetch, InventoryPage inline Giá bán + `Thiếu giá bán` filter. Verified live on a second instance (`:9081`, context `/probe`) with a minted JWT, then cleaned up. **Not done:** decision 4 (HomePage `Hết hàng` confirm) — zero stock does not block selling today, the picker simply shows the item; add the confirm later if wanted. **Requires restarting the dev backend on `:9080`** to load the new classes.

## Test plan
- Backend unit tests (`CoreCatalogSyncServiceTest`, 7 green): create item from service, reuse inactive item, price push updates the active row in place, deactivate both, protected names skipped, shuttle ball unit defaults.
- Backend unit tests (`InventoryServiceImplTest`): `saveItem` pushes price/active into catalog; create without `retailPrice` fails.
- Manual QA: (1) Setup shows vinastar `Còn lại 101 quả`, `Giá bán 26.000`, `Đang bán`; (2) add service + price on Setup → appears in Quản lý kho with same Giá bán and stock 0, appears in HomePage picker; (3) change Giá bán in Quản lý kho → Setup and the player billing price show the new value; (4) delete on Setup → gone from both pages and from the picker, stock ledger intact; restore → back; (5) drag a 0-stock item → confirm dialog → line added and `Còn lại` goes negative; (6) `mvnw compile` + `npm run build` clean, backend restarted on the existing schema (no new changeset).

## Follow-up fix: deadlock on opening the Setup page
Symptom: `GET /api/getSetupServices` failed with `Deadlock found when trying to get lock ... [update inventory_item ...]` and the FE printed that SQL to the user.
Cause: the read path wrote on every load — `ensureItem` saved the item unconditionally and each row healed in its own unordered transaction, so concurrent requests took the same `inventory_item` rows in different orders.
Fix: `ensureItem` is idempotent (no save when nothing changed → steady-state page load is read-only); the heal is one `REQUIRES_NEW` transaction processed in name order (`ensureCatalogLinks`); lock contention is retried (3 attempts, MySQL 1213/1205) and the read path never fails because of it; `GlobalExceptionHandler` answers 503 with "Hệ thống đang xử lý một thao tác khác trên dữ liệu này..." for deadlocks and never returns SQL; the FE interceptor drops technical text and SetupPage shows a retry banner instead of an empty page.
Tests: `CoreCatalogSyncServiceTest` 9 green (incl. read-only heal), `GlobalExceptionHandlerTest` 4 green.
