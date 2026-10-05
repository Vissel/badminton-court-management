package com.badminton.backup.service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BackupCatalog {
    public record Table(String name, List<String> primaryKeys) {
    }

    private final List<Table> tables = List.of(
            table("player", "player_id"), table("session", "session_id"), table("available_player", "ava_id"),
            table("court", "court_id"), table("team", "team_id"), table("shuttle_ball", "shuttle_id"),
            table("game", "game_id"), table("game_shuttle_map", "id"), table("service", "ser_id"),
            table("rent_by_time", "id"), table("debit", "debit_id"), table("debit_summary", "debt_sum_id"),
            table("payment", "payment_id"), table("payment_debit", "payment_debit_id"),
            table("inventory_item", "item_id"), table("purchase_lot", "lot_id"),
            table("stock_movement", "movement_id"), table("role", "role_id"), table("app_user", "user_id"),
            new Table("app_user_role", List.of("user_id", "role_id")), table("refresh_token", "refresh_token_id"),
            table("invoice", "invoice_id"), table("invoice_item", "item_id"), table("invoice_series", "series_key"),
            table("bill_config", "config_id"));

    public List<Table> tables() {
        return tables;
    }

    public void validate(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        for (Table table : tables) {
            try (ResultSet result = metadata.getTables(catalog, null, table.name(), new String[] { "TABLE" })) {
                if (!result.next())
                    throw new IllegalStateException("Required backup table is missing: " + table.name());
            }
            try (ResultSet result = metadata.getColumns(catalog, null, table.name(), "updated_at")) {
                if (!result.next())
                    throw new IllegalStateException("Required updated_at is missing: " + table.name());
            }
        }
        try (ResultSet result = metadata.getTables(catalog, null, "backup_deletion_log", new String[] { "TABLE" })) {
            if (!result.next())
                throw new IllegalStateException("Required backup table is missing: backup_deletion_log");
        }
    }

    private static Table table(String name, String key) {
        return new Table(name, List.of(key));
    }
}
