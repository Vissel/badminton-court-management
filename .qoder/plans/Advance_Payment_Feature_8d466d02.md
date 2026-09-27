# Advance Payment Feature (Trả trước)

## Context

Currently, players are added to a session and "Tiền sân" (court fee) is automatically added as a service. Payment only happens at the end via PayConfirm. This feature allows collecting a partial/advance payment at check-in time, which is then deducted from the final total.

**Flow**: Player added -> popup asks for advance amount -> advance recorded as service line item -> at checkout, PayConfirm shows advance as negative line, reducing the total.

---

## Task 1: Database — Add `advance_payment` column

**File**: `sql/dev-changesetbk/changeset-016-add-advance-payment.sql`

Add a new column `advance_payment` (DECIMAL(10,0), DEFAULT 0) to `available_player` table. This stores the advance amount separately for reporting/querying, while also adding a corresponding service line item in the `services` JSON.

Also add the Liquibase changelog entry in `db.changelog-master.xml`.

---

## Task 2: Backend Entity — Add `advancePayment` field

**File**: `BadmintonCourtManagement/src/main/java/com/badminton/entity/AvailablePlayer.java`

Add:
```java
private Float advancePayment;
```

---

## Task 3: Backend — New endpoint for add-player-with-advance

**Files**:
- `BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/AddPlayerRequest.java` (new) — fields: `playerName` (String), `advanceAmount` (Float, optional, default 0)
- `BadmintonCourtManagement/src/main/java/com/badminton/service/CourtServicesServiceImpl.java` — modify `transactionAddPlayerToCurrentSession` or add a new method that accepts advance amount. When advance > 0:
  1. Save the advance amount on `AvailablePlayer.advancePayment`
  2. Add a service line item `{"serviceName": "Trả trước", "cost": -advanceAmount}` to the player's services JSON (negative cost = deduction)
- `BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java` — update `/addPlayer` endpoint to accept the new `AddPlayerRequest` body instead of raw String.

---

## Task 4: Backend — Update PayServiceImpl to handle advance deduction

**File**: `BadmintonCourtManagement/src/main/java/com/badminton/service/impl/PayServiceImpl.java`

The existing flow already computes total from the service list. Since the advance is stored as a **negative cost line item** in services, the total expense from the frontend will already reflect the deduction. No major backend changes needed for payment logic itself.

However, ensure the `payAmount` stored reflects the **actual collected amount** (total - advance), not the gross total. The frontend will send the correct net total.

---

## Task 5: Backend — Excel export awareness

**File**: `BadmintonCourtManagement/src/main/java/com/badminton/service/impl/ExcelExportService.java`

The "Trả trước" line item with negative cost should be handled in Excel reports:
- Include it in service breakdown so the report shows the advance deduction
- Verify the total revenue calculation correctly accounts for the negative line item

---

## Task 6: Frontend — New `AdvancePaymentDialog.js`

**File**: `bad-court-mana-ui/src/page/dialog/AdvancePaymentDialog.js` (new)

A small MUI Dialog that appears after the cashier adds a player. Contains:
- Title: "Trả trước cho [playerName]"
- A numeric TextField for the advance amount (pre-filled with 0)
- Quick amount buttons (e.g., 50K, 100K, 150K) for fast entry
- Two buttons: "Bỏ qua" (skip, advance = 0) and "Xác nhận" (confirm)

Compact design consistent with existing dialogs.

---

## Task 7: Frontend — Integrate popup in HomePage.js

**File**: `bad-court-mana-ui/src/page/HomePage.js`

Modify `onAddPlayer`:
1. Call the API to add player (same as now)
2. Instead of immediately adding "Tiền sân" service, show `AdvancePaymentDialog`
3. On dialog confirm: add player to `availablePlayers`, then call `handleDropService` for both "Tiền sân" (positive) and "Trả trước" (negative, the advance amount)
4. On dialog skip: add player normally with just "Tiền sân"

New state variables: `showAdvanceDialog`, `pendingPlayerName`

---

## Task 8: Frontend — Update PayConfirm.js to show advance deduction

**File**: `bad-court-mana-ui/src/page/dialog/PayConfirm.js`

When the service list contains a "Trả trước" item (negative cost):
- Show a separate section: "Đã trả trước: -XX,XXX VND" styled differently (e.g., blue/gray, with a small icon)
- Adjust the total display to show: gross total, advance deduction, then net total
- The `expense` passed from HomePage will already be the net amount (since the negative line item is included in the sum)

---

## Task 9: Frontend — Update ServiceDialog.js display

**File**: `bad-court-mana-ui/src/page/dialog/ServiceDialog.js`

In the service list display, render the "Trả trước" line item with a distinct style (e.g., blue text, or a small "Đã trả" chip) so it's visually distinguishable from regular services.

---

## Task 10: i18n — Add translation keys

**Files**: `en/translation.json`, `vi/translation.json`

Keys to add:
- `advancePayment.title` — "Advance Payment" / "Trả trước"
- `advancePayment.skip` — "Skip" / "Bỏ qua"
- `advancePayment.confirm` — "Confirm" / "Xác nhận"
- `advancePayment.paidInAdvance` — "Paid in advance" / "Đã trả trước"

---

## Task 11: Verification

1. Backend: `mvn compile` — verify no compilation errors
2. Frontend: `npm run build` — verify no ESLint errors
3. Manual test: add player with advance -> verify service list shows "Trả trước" with negative cost -> verify PayConfirm shows deduction -> verify final payAmount is correct

---

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Storage | `advancePayment` column + negative service line item | Column for reporting; negative line item for automatic total deduction |
| API change | Modify `/addPlayer` to accept `{playerName, advanceAmount}` | Minimal surface change, backward-compatible (advanceAmount defaults to 0) |
| Service name | "Trả trước" | Consistent with existing "Tiền sân" naming pattern |
| Deduction approach | Negative cost in services JSON | Leverages existing total calculation — no PayService logic changes needed |
