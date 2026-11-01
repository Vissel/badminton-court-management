-- ============================================================================
-- OPTIONAL one-time tidy-up of a LEGACY database's Liquibase bookkeeping.
--
-- NOT REQUIRED FOR DEPLOYMENT: since changesets 001-028 are frozen with
-- validCheckSum 1:any and changeset 025 auto-skips migrated databases, the
-- app boots on any old database with zero manual steps. This script exists
-- only to make the bookkeeping history clean again afterwards.
--
-- METADATA ONLY: this script edits the `databasechangelog` table and nothing
-- else. No application table is created, altered, dropped or truncated, and
-- no business row is read or written — old data is 0-impact.
--
-- WHY IT EXISTS
--   The legacy database (e.g. `bad-court-management-db`) was migrated with an
--   older changelog lineage. On the first boot of the new release Liquibase
--   aborts with "Validation Failed: N changesets check sum" because files
--   004/016/017/018/019/023/026 changed after the legacy DB executed them,
--   and changeset 025 would be re-run although its statements were already
--   applied under the old `db/changelog/rbac-jwt.sql` lineage (re-running it
--   fails: `player.password` was dropped).
--
-- WHAT IT DOES
--   1. Rewrites the stored checksums of the current master-lineage changesets
--      (001-028) to the values this release computes, so validation passes
--      and full checksum protection is restored from this point on.
--   2. Registers changeset 025 (rbac-jwt.sql) as already executed (MARK_RAN).
--   3. Removes obsolete bookkeeping rows from the pre-restructure lineage.
--
--   After this script, the first boot of the new WAR applies only the
--   additive changesets 029-031 (2 new columns, 1 new table, and the
--   idempotent catalog-to-inventory item relink).
--
-- USAGE (optional, against a legacy database; also a recovery path if a
--        boot ever reports a checksum mismatch for a 029+ changeset):
--   mysql -u <user> -p <database> < legacy-changelog-rebase.sql
--
-- Idempotent: safe to run more than once.
-- ============================================================================

-- 1. Rewrite stored checksums to the values computed by this release.
UPDATE databasechangelog SET md5sum='9:25bf05c635c7c4e62e278674442d4f3a' WHERE id='001' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:9506755dba12d2c597b38700171e87dd' WHERE id='002' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:2c617358d680fd288ffa51c20c4cf46a' WHERE id='003' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:636797a9595cfad01c953f15d8745101' WHERE id='004' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:da670d5eeb80998df09bd6c71e75d33a' WHERE id='005' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:bddbeb62554b695149467e210e4a9a8e' WHERE id='016' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:6ad170f41dddf29cd9c452d5e8fce82b' WHERE id='017' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:60b452f018f18416edcac7829d690970' WHERE id='018' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:f3f97f7a338966cfc7afeb17befdd229' WHERE id='019' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:854508528e06661eed773e63f0c49fa0' WHERE id='020' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:536fff5574e3b5a5d72631dcfdd6f384' WHERE id='021' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:7047fe699b74b2a04656f29ab7254926' WHERE id='022' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:4ca9f449786b6d8f35df404258f16e56' WHERE id='023' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:9bc48c1ec77177c505ed4eabd3b7531a' WHERE id='024' AND author='thach' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:c563c12c401f9efc425be1ad81bc70cf' WHERE id='026' AND author='devin' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:d1fc29d7b10daa89e4311c43d4b558dd' WHERE id='027' AND author='devin' AND filename='db/changelog/db.changelog-master.xml';
UPDATE databasechangelog SET md5sum='9:96c7c416c938a80b0eb3d1a3096feea6' WHERE id='028' AND author='devin' AND filename='db/changelog/db.changelog-master.xml';

-- 2. Register changeset 025 under the current lineage as already executed.
INSERT INTO databasechangelog
    (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum,
     description, comments, tag, liquibase, contexts, labels, deployment_id)
SELECT '025', 'thach', 'db/changelog/db.changelog-master.xml', NOW(),
       COALESCE(MAX(orderexecuted), 0) + 1,
       'MARK_RAN', '9:3293fa06ed9530f0a914953cdb724dd3', 'empty', '', NULL,
       '4.31.1', NULL, NULL, NULL
FROM databasechangelog
WHERE NOT EXISTS (
    SELECT 1 FROM databasechangelog
    WHERE id='025' AND author='thach' AND filename='db/changelog/db.changelog-master.xml'
);

-- 3. Remove obsolete bookkeeping rows of the pre-restructure master lineage
--    (these change-set ids no longer exist in the current changelog).
DELETE FROM databasechangelog
WHERE filename='db/changelog/db.changelog-master.xml'
  AND id IN ('006','007','008','009','010','011','012','012.1','013','013.1','013.2','013.3','014','015','015.2');
