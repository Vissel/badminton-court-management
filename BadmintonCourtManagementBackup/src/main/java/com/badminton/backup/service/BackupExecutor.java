package com.badminton.backup.service;

import com.badminton.backup.config.BackupProperties;
import com.badminton.backup.model.ArchiveResult;
import com.badminton.backup.model.BackupScope;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.springframework.stereotype.Service;

@Service
public class BackupExecutor {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("HHmmss_SSS").withZone(ZoneOffset.UTC);
    private final BackupSourceDatabase source;
    private final BackupCatalog catalog;
    private final BackupProperties properties;
    private final ObjectMapper mapper;

    public BackupExecutor(BackupSourceDatabase source, BackupCatalog catalog, BackupProperties properties, ObjectMapper mapper) {
        this.source = source; this.catalog = catalog; this.properties = properties; this.mapper = mapper;
    }

    public ArchiveResult execute(long triggerId, BackupScope scope, Map<String, Instant> fromCutoffs, Instant cutoff)
            throws Exception {
        Path directory = properties.storage().basePath().resolve(DAY.format(cutoff));
        Files.createDirectories(directory);
        String basename = scope.name().toLowerCase() + "_" + FILE_TIME.format(cutoff) + "_" + triggerId + ".json.gz";
        Path target = directory.resolve(basename);
        Path temporary = directory.resolve(basename + ".tmp");
        long rows;
        try (Connection connection = source.connection()) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            catalog.validate(connection);
            rows = writeArchive(connection, temporary, triggerId, scope, fromCutoffs, cutoff);
            connection.commit();
        } catch (Exception exception) {
            Files.deleteIfExists(temporary);
            throw exception;
        }
        movePublished(temporary, target);
        return new ArchiveResult(target, Files.size(target), rows, sha256(target));
    }

    private long writeArchive(Connection connection, Path path, long triggerId, BackupScope scope,
            Map<String, Instant> fromCutoffs, Instant cutoff) throws Exception {
        long total = 0;
        JsonFactory factory = mapper.getFactory();
        try (OutputStream file = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW);
                GZIPOutputStream gzip = new GZIPOutputStream(new BufferedOutputStream(file));
                JsonGenerator json = factory.createGenerator(gzip)) {
            json.setCodec(mapper);
            json.writeStartObject();
            json.writeNumberField("formatVersion", 1);
            json.writeNumberField("triggerId", triggerId);
            json.writeStringField("scope", scope.name());
            json.writeStringField("toInclusive", cutoff.toString());
            json.writeObjectFieldStart("tables");
            for (BackupCatalog.Table table : catalog.tables()) {
                Instant from = fromCutoffs.getOrDefault(table.name(), Instant.EPOCH);
                json.writeObjectFieldStart(table.name());
                if (scope == BackupScope.INCREMENTAL) json.writeStringField("fromExclusive", from.toString());
                json.writeArrayFieldStart("upserts");
                total += streamRows(connection, table.name(), scope, from, cutoff, json);
                json.writeEndArray();
                json.writeArrayFieldStart("deletes");
                if (scope == BackupScope.INCREMENTAL) total += streamDeletes(connection, table.name(), from, cutoff, json);
                json.writeEndArray();
                json.writeEndObject();
            }
            json.writeEndObject();
            json.writeEndObject();
        }
        return total;
    }

    private long streamRows(Connection connection, String table, BackupScope scope, Instant from, Instant to,
            JsonGenerator json) throws SQLException, IOException {
        String sql = "SELECT * FROM `" + table + "`";
        if (scope == BackupScope.INCREMENTAL) sql += " WHERE updated_at > ? AND updated_at <= ? ORDER BY updated_at";
        long count = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            statement.setFetchSize(500);
            if (scope == BackupScope.INCREMENTAL) {
                statement.setTimestamp(1, Timestamp.from(from));
                statement.setTimestamp(2, Timestamp.from(to));
            }
            try (ResultSet result = statement.executeQuery()) {
                ResultSetMetaData metadata = result.getMetaData();
                while (result.next()) {
                    json.writeStartObject();
                    for (int column = 1; column <= metadata.getColumnCount(); column++)
                        json.writeObjectField(metadata.getColumnLabel(column), result.getObject(column));
                    json.writeEndObject();
                    count++;
                }
            }
        }
        return count;
    }

    private long streamDeletes(Connection connection, String table, Instant from, Instant to, JsonGenerator json)
            throws SQLException, IOException {
        String sql = "SELECT record_key, deleted_at FROM backup_deletion_log WHERE table_name = ? "
                + "AND deleted_at > ? AND deleted_at <= ? ORDER BY deleted_at, deletion_id";
        long count = 0;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setTimestamp(2, Timestamp.from(from));
            statement.setTimestamp(3, Timestamp.from(to));
            statement.setFetchSize(500);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    json.writeStartObject();
                    json.writeFieldName("key");
                    json.writeRawValue(result.getString("record_key"));
                    json.writeObjectField("deletedAt", result.getTimestamp("deleted_at"));
                    json.writeEndObject();
                    count++;
                }
            }
        }
        return count;
    }

    private void movePublished(Path from, Path to) throws IOException {
        try { Files.move(from, to, StandardCopyOption.ATOMIC_MOVE); }
        catch (AtomicMoveNotSupportedException exception) { Files.move(from, to); }
    }

    private String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (DigestInputStream input = new DigestInputStream(Files.newInputStream(file), digest)) {
            input.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
