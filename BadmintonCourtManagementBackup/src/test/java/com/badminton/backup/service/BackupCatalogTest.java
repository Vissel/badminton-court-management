package com.badminton.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class BackupCatalogTest {
    private final BackupCatalog catalog = new BackupCatalog();

    @Test
    void catalogHasUniqueTablesAndCompositeUserRoleKey() {
        assertThat(catalog.tables()).hasSize(25);
        assertThat(new HashSet<>(catalog.tables().stream().map(BackupCatalog.Table::name).toList())).hasSize(25);
        assertThat(catalog.tables()).filteredOn(table -> table.name().equals("app_user_role"))
                .singleElement().extracting(BackupCatalog.Table::primaryKeys)
                .isEqualTo(java.util.List.of("user_id", "role_id"));
    }
}
