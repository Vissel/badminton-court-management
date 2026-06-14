# Edit Available Player

## Goal and Assumption

Allow an administrator to **rename an available player by clicking the player in the available-player list**, reusing the existing `ServiceDialog` opened from `HomePage.js`. I am assuming “edit available player” means editing the player’s name; if you meant pay amount/type or another field, adjust before approving.

## Files to Modify

### Backend

- `/Users/user/eclipse-workspace/badminton-court-management/BadmintonCourtManagement/src/main/java/com/badminton/requestmodel/AvaPlayerDTO.java`
  - Add an `oldPlayerName` field so the API can locate the current active-session player while saving the new `playerName`.

- `/Users/user/eclipse-workspace/badminton-court-management/BadmintonCourtManagement/src/main/java/com/badminton/service/CourtServicesServiceImpl.java`
  - Add a transactional method such as `updateAvailablePlayer(AvaPlayerDTO request)`.
  - Validate:
    - current name is not blank
    - new name is not blank
    - an active-session `AvailablePlayer` exists for the old name
    - no other active available player already uses the new name
  - Update the linked `Player.playerName` and `Player.password` consistently with the existing add-player behavior.
  - Save through `userRepo` / existing persistence flow.

- `/Users/user/eclipse-workspace/badminton-court-management/BadmintonCourtManagement/src/main/java/com/badminton/controller/CourtManagementController.java`
  - Add `POST /court-mana/updateAvailablePlayer` accepting `AvaPlayerDTO` and returning `Result<Boolean>` via `ResponseConvertor.convert(...)`, matching the existing `/addPlayer` response pattern.

### Frontend

- `/Users/user/eclipse-workspace/badminton-court-management/bad-court-mana-ui/src/page/dialog/ServiceDialog.js`
  - Reuse the existing dialog; no new dialog/page.
  - Add optional props like `onUpdatePlayerName` and `canEditPlayerName`.
  - Show the current player name as it does today, plus a compact inline edit mode near the title:
    - “Sửa tên” button
    - `TextField` for the new name
    - Save/Cancel buttons
  - Keep service editing, pay, and delete behavior unchanged.
  - Hide name editing when `hideActions === true`, so court-area player clicks remain restricted/view-only as currently designed.

- `/Users/user/eclipse-workspace/badminton-court-management/bad-court-mana-ui/src/page/HomePage.js`
  - Add `handleUpdatePlayerName(oldName, newName)`.
  - Call `api.post('/court-mana/updateAvailablePlayer', { oldPlayerName: oldName, playerName: newName })`.
  - On success, update local state without page refresh:
    - replace the name in `availablePlayers`
    - move `playerServiceMap[oldName]` to `playerServiceMap[newName]`
    - update `selectedPlayer` to the new name so the dialog stays open on the edited player
  - Pass the handler to `ServiceDialog` only for available-player clicks.

## Key Decisions

- **No DB changes**: `available_player` already references `player`; the editable field lives in `player.playerName`.
- **No new UI creation**: reuse the existing click-triggered `ServiceDialog` and add only an inline edit affordance inside it.
- **Available players only**: keep court-area players non-editable because existing behavior sets `hideActions=true` for court players and intentionally restricts actions.
- **Duplicate protection**: backend remains source of truth; frontend may also guard against no-op/blank edits for better UX.

## Verification

- Run backend compile from `BadmintonCourtManagement`: `./mvnw compile`.
- Run frontend build from `bad-court-mana-ui`: `npm run build`.
- Manual flow to verify:
  - click an available player
  - edit name and save
  - confirm name changes in the available-player list
  - confirm services remain attached after rename
  - confirm duplicate/blank name is rejected
  - confirm clicking a court-area player does not show the name-edit action.