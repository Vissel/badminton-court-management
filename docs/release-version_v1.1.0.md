# Release Version v1.1.0

## Summary
Enhanced the court management UI with player search & add flows, service search in dialogs, data consistency fixes, and UX improvements across 5 files.

---

## File Changes

### `HomePage.js`
- **Fix: scrollRef timing** — Moved scroll-to-bottom logic inside `fetchCourtInfor()` after data loads, wrapped with `requestAnimationFrame` to ensure DOM is updated before scrolling.
- **Fix: court-player services empty after refresh** — While processing `gameDTOs`, now also extracts `playerInArea.serviceResponses` (already present in backend response) and populates `playerServiceMap`, so court-assigned players' services show correctly after page load without requiring a return to the player pool.
- **Court-player click handler** — Added `handleCourtPlayerClick` that opens `ServiceDialog` with `hideActions=true` (pay/delete buttons hidden). Passed as `onClickPlayer` to both Court instances.
- **Prop plumbing** — Passed `availablePlayers` to both Court instances (→ DropZone), `handleCourtPlayerClick` as `onClickPlayer`, and `serviceOptions={services}` to ServiceDialog.

### `PlayerArea.js`
- **Duplicate player warning** — Added `duplicateWarning` state. When user types/enters a name already in `availablePlayers`, the TextField shows red error styling and a `"Người chơi đã tồn tại"` caption (conditionally rendered to not waste space).
- **Dual-mode TextField (Add/Search)** — Toggle button with `SearchIcon` / `CloseIcon` inside `InputAdornment` switches between add mode and search mode.
  - *Add mode* (default): Enter adds the player, duplicate detection active.
  - *Search mode*: Typing filters `availablePlayers` (case-insensitive `includes`), list shows matched/total count, drag-and-drop remains fully functional.
  - Search input auto-focuses on toggle.

### `DropZone.js` *(fully rewritten)*
- **Click-to-search player** — Clicking an empty, unlocked DropZone opens a compact search `TextField` with a `+` button inside the zone.
- **Live filtering** — Typing shows a `Paper` dropdown with matching available players (case-insensitive `includes`).
- **Selection & add** — Clicking a dropdown item fills the field. Pressing Enter or clicking `+` calls `onDropPlayer(name, courtId, areaKey)` (same API as drag-and-drop). Auto-adds if exactly 1 match.
- **Click-away dismiss** — `ClickAwayListener` closes search when clicking outside.
- **Player clicks** — Court players are now `onClickPlayer` targets, opening ServiceDialog.
- **Coexistence with DnD** — Drag-and-drop remains fully unchanged. DnD placement automatically closes search if open.

### `Court.js`
- Accepts `availablePlayers` and `onClickPlayer` props, passes both through to each `DropZone`.

### `ServiceDialog.js`
- **`hideActions` prop** — When `true` (court-player context), the "Thanh toán" and "Xoá + không thanh toán" buttons are hidden. Service list viewing and adding remains available.
- **Service search dropdown** — New `serviceOptions` prop (the draggable services list). As user types the service name, a `Paper` dropdown shows matching services with name + price. Clicking fills both service name and price TextFields at once.

### `PayConfirm.js` *(fully redesigned)*
- **Modern banking-style UI** — Replaced raw `DialogTitle` text with a structured layout:
  - Colored header banner with circular icon (green checkmark for payment, red cancel for cancellation).
  - Hero amount display (`h3` formatted with `VND` currency label).
  - Player name as recipient (with `PersonOutlinedIcon`).
  - Receipt-style service breakdown with individual costs.
  - Bold "Tổng cộng" total footer.
  - Two full-width, rounded action buttons ("Huỷ" / "Xác nhận thanh toán" or "Xác nhận huỷ").
- **MUI v9 icon fix** — `CheckCircleOutlineIcon` → `CheckCircleIcon`, `PersonOutlineIcon` → `PersonOutlinedIcon` to match MUI v9 naming conventions.
