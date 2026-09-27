# Inventory Management Redesign

Goal: manage goods (shuttle balls, drinks/goods, sellable services) as real inventory —
wholesale intake, retail sales, stock on hand, margin, and dates.

---

## 1. Why the current design is not enough

- `shuttle_ball` / `service` are **price catalogs only**: `name + cost (retail)`. No quantity, no purchase cost, no intake date.
- `GameShuttleMap.shuttleNumber` counts balls billed per game but never decrements any stock.
- `AvailablePlayer.services` is a JSON string of `ServiceDTO{name, cost}` — no FK, no quantity, no stock link.
- Product import/export moves name+price rows only.
- Catalog rows are ephemeral by convention: price update = deactivate + insert new row (history preservation for `GameShuttleMap`). Stock must therefore attach to a stable identity, **not** to catalog rows.

## 2. New domain model

### `inventory_item` — stable stock identity
| Column | Note |
|---|---|
| `item_id` PK | stable identity (name + type) |
| `name` | unique per `item_type` |
| `item_type` | `SHUTTLE_BALL` \| `GOODS` \| `SERVICE_STOCKABLE` |
| `unit` | display: quả, ống, chai... |
| `retail_price` | default sell price (mirrors catalog `cost`) |
| `is_active`, `created_date` | |

### `purchase_lot` — wholesale intake (the "big number" + date)
| Column | Note |
|---|---|
| `lot_id` PK | |
| `item_id` FK | |
| `purchase_date` | intake date |
| `quantity` | wholesale qty |
| `unit_cost` | wholesale price per unit |
| `total_cost` | qty × unit_cost |
| `supplier`, `note` | optional |

### `stock_movement` — ledger (source of truth for stock)
| Column | Note |
|---|---|
| `movement_id` PK | |
| `item_id` FK | |
| `movement_type` | `PURCHASE_IN` \| `GAME_CONSUMPTION` \| `RETAIL_SALE` \| `ADJUSTMENT` \| `RETURN` |
| `quantity_delta` | signed |
| `ref_type` / `ref_id` | `game_id`, payment/debit id, `lot_id` |
| `note`, `created_date` | |

**Stock on hand** = `SUM(quantity_delta)` per item; optionally cache `stock_on_hand` on
`inventory_item` updated in the same transaction.

**Margin** = `retail_price − avg_unit_cost` (weighted average of remaining stock; simpler than FIFO, recommended).

### Catalog linkage
- Add `item_id` FK to `shuttle_ball` and `service` (nullable).
- The existing "deactivate + insert" price-history convention keeps working: the new price row re-points to the same `item_id`. Stock is tracked per `item_id`, immune to catalog churn.
- Protected rows (`costInPerson`, `rentByTime`) and non-stockable services get **no** `item_id` — they stay pure pricing.

## 3. Flow changes

- **Wholesale intake**: `POST /api/inventory/purchases` → insert `purchase_lot` + `PURCHASE_IN` movement. Bulk variant: extend Excel import with a `StockIntake` sheet (`Tên | Số lượng | Giá nhập | Ngày nhập`) or separate `POST /api/inventory/purchases/import`.
- **Game consumption**: on game FINISH (`CourtServicesService.changeGameState` → `gameCalculator.calculateGameResult`), write `GAME_CONSUMPTION` movements = `shuttleNumber` per `GameShuttleMap`. Warn/block on START if stock < requested qty.
- **Retail sale**: extend `ServiceDTO` with `serviceId`/`itemId` + `quantity`; on player checkout/payment, write `RETAIL_SALE` movements for stockable items. Old JSON rows (no new fields) parse leniently — treated as qty 1, non-stockable.
- **Stock take / correction**: `POST /api/inventory/adjustments` → `ADJUSTMENT` movement with reason.

## 4. API surface (all `rootuser`-gated, same as `ProductsController`)

- `GET /api/inventory/items` — item, stockOnHand, avgCost, retailPrice, margin, lowStock flag
- `POST /api/inventory/items`, `PUT /api/inventory/items/{id}` — goods catalog CRUD
- `POST /api/inventory/purchases` — record intake
- `GET /api/inventory/movements?itemId=&from=&to=` — ledger
- `POST /api/inventory/adjustments` — corrections
- `GET /api/inventory/export` — xlsx stock/margin report (reuse `ProductExcelWriter` pattern)

## 5. Frontend scope

The FE app is deployed separately in Tomcat `webapps` (not in this repo) — work happens in that codebase.

- **Inventory page** (nav-gated to `rootuser`, same pattern as the existing admin-only product import UI): item list with stock on hand, avg cost, retail price, margin, low-stock badge.
- **Purchase intake form**: item picker (autocomplete), quantity, unit cost, date, supplier, note → `POST /api/inventory/purchases`; bulk intake via Excel upload (reuse the import preview/commit UX).
- **Stock ledger view**: per-item movement history (type, delta, ref, date).
- **Adjustment dialog**: quantity correction + reason.
- **Game/court UI**: low-stock warning when starting a game; shuttle selector shows remaining stock.
- **Service picker**: quantity input for stockable goods; out-of-stock items disabled.

## 6. Phasing (fits plan2026.md sprint model)

| Phase | Scope | Effort |
|---|---|---|
| 1 | Liquibase: 3 new tables + `item_id` columns + backfill items from active catalog names; entities/repos; items + purchases + stock view endpoints; FE: inventory page skeleton + intake form | 3 sessions |
| 2 | Stock deduction: game finish (balls), payment (goods); ServiceDTO `itemId`/`quantity`; low-stock warnings; FE: ledger view + low-stock badge + quantity in service picker | 2–3 sessions |
| 3 | Intake Excel import, inventory export, margin report, stock-take UI data; FE: adjustment dialog + margin report view | 2 sessions |

**Total: ~7–8 sessions (~4 weeks), BE + FE combined.**

## 7. Open decisions

- Are retail goods sold through the existing player-services billing flow (`AvailablePlayer.services`)? **Assumed yes.**
- `RentShuttleDTO` rental balls — track as `RENT_OUT`/`RETURN` movements or exclude from stock? Recommend exclude initially.
- `cost` is `float` — new tables should use `BIGDECIMAL`; migrate catalog `cost` later (separate change, risky).
- Goods vs Service sheet in Excel: keep `Service` sheet for non-stockable pricing rows, add `Goods` sheet for stockable items.

---

## 8. Implementation status (Sep 2026) — FE + BE done

Backend (`/api/inventory/**`) and product catalog import/export (`/api/products/**`) are implemented and rootuser-gated server-side (`InventoryController`, `ProductsController`). Frontend in `bad-court-mana-ui`:

### Product catalog — `/products` ("Quản lý hàng")

| File | Change |
|---|---|
| `src/page/ProductPage.js` | Tabs `Loại cầu` / `Đồ uống / Đồ ăn`; inline add, delete/restore, cost edit (delete+insert per catalog convention); toolbar: `Nhập file` / `Xuất file` / `Tải mẫu` |
| `src/page/dialog/ProductImportDialog.js` | `.xlsx` picker → preview → diff (`Thêm`/`Cập nhật`/`Khôi phục`/`Bỏ qua`/`Lỗi`) → token commit |
| `src/api/productApi.js` | Export/template blob download, multipart preview + commit; reuses `/api/getSetupServices` + `/api/updateSetupService` for catalog read/write |
| `src/api/index.js` | Interceptor skips forced `Content-Type: application/json` for `FormData` (multipart boundary fix) |
| `src/context/ProtectedRoute.js` | `requireRoot` prop — non-`rootuser` redirected to `/home` |

### Inventory — `/inventory` ("Quản lý kho")

| File | Change |
|---|---|
| `src/page/InventoryPage.js` | Item table: tên, loại, tồn kho, giá nhập TB, giá bán, lãi; low-stock row highlight + `Sắp hết` chip; per-row ledger / kiểm kê / sửa |
| `src/page/dialog/InventoryItemDialog.js` | Create/edit item (tên, SHUTTLE_BALL/GOODS, đơn vị lẻ + đơn vị nhập sỉ/quy đổi, giá bán) → `POST/PUT /items` |
| `src/page/dialog/PurchaseDialog.js` | Intake form: item autocomplete with current stock/breakdown, toggle đơn vị lẻ/ống, số lượng, giá nhập, ngày nhập, supplier, note → `POST /purchases` |
| `src/page/dialog/StockIntakeDialog.js` | Bulk Excel intake (`StockIntake`: `Tên \| Số lượng \| Giá nhập \| Ngày nhập \| Đơn vị`) → preview → commit |
| `src/page/dialog/StockLedgerDialog.js` | Per-item `stock_movement` history with from/to filters |
| `src/page/dialog/StockAdjustDialog.js` | Kiểm kê: +/- toggle, quantity, mandatory reason, resulting-stock preview |
| `src/api/inventoryApi.js` | Full `/api/inventory/**` client |

### Deviations from this design

- `InventoryItemType` implemented as `SHUTTLE_BALL | GOODS` only — `SERVICE_STOCKABLE` (§2) was dropped; FE follows the actual enum.
- Two-level units (Sep 2026): ledger counts in `unit` (base, e.g. `quả`); `package_unit`/`units_per_package` (ống ×12 default for SHUTTLE_BALL) are an intake-time conversion. `purchase_lot` keeps bought `unit`/`quantity` + `base_quantity`; intake APIs accept `PACKAGE`/`BASE`/label, blank → package. StockIntake sheet gains optional 5th column `Đơn vị`.
- Picker-facing stock now unblocked: `ServiceResponse` and `ShuttleBallResponse` expose `itemId`, `stockOnHand` (base units), and `lowStock` from the linked `InventoryItem`.
- Retail/service stock flow uses Option A (Sep 2026): `addServiceToAvailablePlayer` writes `RETAIL_SALE`, removals / update downgrades / player or session cleanup write `RETURN`, and payment no longer touches stock.
- No separate margin report screen — margin is shown per-item in the table and in `/api/inventory/export`.
