# BadmintonCourtManagement Backup Service

Standalone JDK 25 service for full and incremental logical backups of the BCM MySQL database.

Detailed architecture and software design: [Software Design Description](SOFTWARE_DESIGN_DESCRIPTION.md).

## Responsibilities

- Read BCM through a read-only datasource.
- Store operational metadata in a separate `backup_meta` schema.
- Produce gzip JSON archives and SHA-256 checksums.
- Accept scheduled, ROOT-only REST, and RabbitMQ triggers.
- Publish completion and failure status events.

BCM business flows do not depend on this service. BCM only supplies timestamps, deletion tombstones, and JWTs.

## Requirements

- JDK 25
- MySQL 8
- RabbitMQ (disable with `backup.rabbitmq.enabled=false` when unused)
- The BCM database migration `027-backup-change-tracking.sql`

## Configuration

Set secrets through environment variables:

```bash
export BACKUP_META_URL='jdbc:mysql://localhost:3306/backup_meta?serverTimezone=UTC'
export BACKUP_META_USERNAME='backup_meta_user'
export BACKUP_META_PASSWORD='...'
export BCM_BACKUP_SOURCE_URL='jdbc:mysql://localhost:3306/badminton-db?serverTimezone=UTC'
export BCM_BACKUP_SOURCE_USERNAME='badminton_backup_reader'
export BCM_BACKUP_SOURCE_PASSWORD='...'
export BCM_JWT_PUBLIC_KEY='file:/secure/path/public_key.pem'
export BACKUP_PATH='/var/backups/badminton'
```

The source account should have only `SELECT` permission. The metadata account needs DML permissions and Liquibase migration permission for `backup_meta`.

## Build and run

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
./mvnw clean verify
./mvnw spring-boot:run
```

If the sibling BCM Maven wrapper is used:

```bash
../BadmintonCourtManagement/mvnw -f pom.xml clean verify
```

## REST

All endpoints require a BCM bearer JWT with `ROLE_ROOT`.

```http
POST /api/backup/triggers
Content-Type: application/json
Authorization: Bearer <token>

{"scope":"INCREMENTAL"}
```

Additional endpoints:

- `GET /api/backup/triggers?page=0&size=20`
- `GET /api/backup/triggers/{id}`
- `GET /api/backup/triggers/{id}/files`
- `GET /api/backup/watermarks`

The trigger endpoint returns `202 Accepted` and a trigger identifier.

## RabbitMQ

Default trigger queue: `badminton.backup.trigger.queue`.

```json
{"requestId":"request-123","scope":"INCREMENTAL"}
```

Status routing keys on `badminton.backup.exchange`:

- `badminton.backup.status.completed`
- `badminton.backup.status.failed`

Duplicate request IDs return the existing trigger and are not executed twice.

## Schedules

Defaults use `Asia/Ho_Chi_Minh`:

- Daily incremental: midnight
- Weekly incremental: Monday midnight
- Monthly full: first day of month at 00:30

All cron values and the timezone are configurable.

## Archive safety

The executor streams rows, writes a `.tmp` archive, closes and checksums it, and atomically moves it to the final path. Watermarks advance only after successful publication. Only one backup may run at a time.
