# Feature Backup Qualification

## Scope

This document verifies backup coverage for the feature sets described as:

1. Online Badminton Court Management with RBAC.
2. Tax Billing Export for Badminton Court Management.

The assessment compares current BCM entities and Liquibase migrations with the backup catalog, BCM change-tracking migration, and archive behavior.

## Qualification result

| Feature set | Result | Required backup application change |
|---|---|---|
| Online management and RBAC | Qualified | None; explicit regression tests added |
| Tax billing, export, printing, and e-invoice state | Qualified | None; explicit regression tests added |

Both feature sets are covered by the current 25-table backup catalog. Because the executor uses `SELECT *`, newly added columns on covered tables are included automatically without changing Java row mappings.

## Online management and RBAC

### Persistent state

| Data | BCM storage | Backup coverage | Incremental behavior |
|---|---|---|---|
| Staff account identity and status | `app_user` | Included | `updated_at` captures status, password, display-name, and username changes |
| Role definitions | `role` | Included | Role changes are upserts; hard deletes produce tombstones |
| User-role assignments | `app_user_role` | Included with composite key `(user_id, role_id)` | Inserts are upserts and removals produce composite-key tombstones |
| Refresh-token rotation/revocation | `refresh_token` | Included | Inserts and revoked-state updates are captured; deletes produce tombstones |
| Court players | `player` | Included | Player mutations and deletes are captured |
| Session and court workflows | `session`, `available_player`, `court`, `team`, `game`, `game_shuttle_map` | Included | Inserts, state transitions, allocation changes, and deletes are captured |

### Security compatibility

The backup REST API intentionally remains restricted to `ROLE_ROOT`. The new BCM hierarchy allowing administrators to manage coordinator accounts does not grant administrators permission to trigger or inspect backups.

BCM JWTs remain compatible because both applications use:

- RS256 signatures.
- The same public key for validation.
- A `roles` claim.
- The `ROLE_` authority prefix.

### Cascade-delete note

MySQL foreign-key cascades do not always execute child-table delete triggers. Deleting an `app_user` may therefore record the parent tombstone without a separate `app_user_role` or `refresh_token` tombstone for every cascaded row. This does not prevent reconstructing the final state when a future restore process applies parent deletion semantics, but the restore implementation must honor foreign-key cascades.

## Tax billing, exports, printing, and e-invoice

### Persistent state

| Data | BCM storage | Backup coverage |
|---|---|---|
| Bill header, buyer/tax snapshot, VAT, totals, payment channel | `invoice` | Included |
| E-invoice number, reference, URL, status, timestamp, and last error | `invoice` | Included |
| Print count and last print time | `invoice` | Included |
| Immutable bill lines | `invoice_item` | Included |
| Sequential numbering state | `invoice_series` | Included |
| Venue, VAT, printer, paper, and e-invoice settings | `bill_config` | Included |
| Payment records | `payment` | Included |
| Payment-to-debit allocation | `payment_debit` | Included |
| Debt state | `debit`, `debit_summary` | Included |

The `einvoice_error` column added after the original backup migration is automatically included because `invoice` is exported with `SELECT *`. Updates to this column also update `invoice.updated_at`, so incremental backups include the changed row.

### Generated artifacts

The following are generated responses and are not independent persistent state:

- Bill Excel exports.
- Locally rendered bill PDFs.
- ESC/POS network-printer output.
- Browser print output.

They do not require separate backup. They can be regenerated from `invoice`, `invoice_item`, and `bill_config`.

Provider-generated e-invoice PDFs are external artifacts. BCM stores provider identifiers and status needed to download them again, but the binary PDF is not copied into the database backup.

### Secrets and external provider state

MISA credentials under `einvoice.misa.*` are deliberately excluded from backups. They are deployment secrets and must be restored from the environment or secrets manager.

The backup also cannot preserve provider-side state that is not represented in BCM. Operational recovery therefore requires:

1. Restore BCM invoice and configuration rows.
2. Restore MISA credentials from secure configuration.
3. Reconcile issued invoices with the provider using `einvoice_ref`, `einvoice_no`, and status APIs.

## Change-tracking verification

Each qualified table has:

- `created_at`.
- `updated_at` managed by MySQL.
- An index on `updated_at`.
- An `AFTER DELETE` tombstone trigger.

`BackupCatalog.validate` checks every configured table and its `updated_at` column at startup and before execution. It also checks `backup_deletion_log`.

## Regression protection

`BackupCatalogTest` now explicitly asserts that the catalog contains:

- RBAC: `role`, `app_user`, `app_user_role`, `refresh_token`.
- Billing: `invoice`, `invoice_item`, `invoice_series`, `bill_config`.
- Financial dependencies: `payment`, `payment_debit`, `debit`, `debit_summary`.

## Conditions for future qualification

A new feature requires a backup update when it introduces any of the following:

- A new persistent database table.
- Persistent files or object-storage artifacts that cannot be regenerated.
- External state without a local reconciliation identifier.
- A table without `updated_at` or delete tracking.
- A new secret source that needs an operational restore procedure.

Adding columns to an existing catalog table normally requires no backup code change, but the column must participate in MySQL's row update so `updated_at` changes.
