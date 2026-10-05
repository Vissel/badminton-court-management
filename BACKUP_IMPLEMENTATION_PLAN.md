# BadmintonCourtManagement Backup Implementation Plan

## Architecture

The backup capability is a separate JDK 25 Spring Boot service in `BadmintonCourtManagementBackup`. BCM remains responsible for business data, JWT issuance, and database change tracking. The backup service has read-only access to BCM and writable access to a separate `backup_meta` schema.

## Boundary

### BCM
- Owns all business tables.
- Maintains `created_at` and `updated_at` columns.
- Records hard deletes in `backup_deletion_log` through database triggers.
- Issues JWTs containing the `roles` claim.

### Backup service
- Validates BCM JWTs and allows `ROLE_ROOT` to administer backups.
- Runs full and incremental exports.
- Stores trigger, file, watermark, and RabbitMQ request metadata in `backup_meta`.
- Writes gzip JSON archives using temporary files and atomic publication.
- Calculates SHA-256 checksums.
- Owns scheduled, REST, and RabbitMQ trigger adapters.

## Common trigger flow

All adapters call `BackupOrchestrator.requestBackup`; adapters never call the executor directly.

```text
Schedule ----+
REST --------+--> BackupOrchestrator --> lock --> RUNNING metadata
RabbitMQ ----+                              |
                                            +--> BackupExecutor
                                            +--> archive + checksum
                                            +--> watermark update
                                            +--> COMPLETED/FAILED
                                            +--> RabbitMQ status
```

## Trigger points

- Daily incremental, weekly incremental, and monthly full schedules; cron and timezone are configurable.
- `POST /api/backup/triggers`, protected by a BCM JWT with `ROLE_ROOT`.
- RabbitMQ trigger queue with idempotency based on `requestId`.
- No startup backup.

## Full backup

Stream every configured table from a repeatable-read, read-only source transaction to one gzip JSON archive. Publish the archive only after all tables finish. A failed table fails the complete run.

## Incremental backup

Use a database cutoff and select rows where `previous_cutoff < updated_at <= cutoff`. Include tombstones where `previous_cutoff < deleted_at <= cutoff`. Advance the watermark only after the archive has been durably published and metadata saved.

## Tables

`player`, `session`, `available_player`, `court`, `team`, `shuttle_ball`, `game`, `game_shuttle_map`, `service`, `rent_by_time`, `debit`, `debit_summary`, `payment`, `payment_debit`, `inventory_item`, `purchase_lot`, `stock_movement`, `role`, `app_user`, `app_user_role`, `refresh_token`, `invoice`, `invoice_item`, `invoice_series`, and `bill_config`.

## Delivery phases

1. Standalone JDK 25 service and typed configuration.
2. BCM timestamp and deletion tracking migration.
3. Metadata schema and persistence.
4. Streaming full and incremental executor.
5. Shared orchestration and overlap protection.
6. Scheduled, ROOT REST, and RabbitMQ adapters.
7. Unit/build verification and operational documentation.

## Security

No credentials or private keys are committed. Source credentials should belong to a SELECT-only MySQL account. The service receives the BCM RSA public key through configuration. Metadata credentials are separate.

## Archive contract

Archives contain format version, scope, cutoff window, table upserts, and table tombstones. They are logical data exports; restoring requires a controlled importer that applies parent tables before children and tombstones after upserts.
