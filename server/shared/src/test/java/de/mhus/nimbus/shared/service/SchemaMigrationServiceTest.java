package de.mhus.nimbus.shared.service;

import de.mhus.nimbus.shared.persistence.SchemaMigrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for SchemaMigrationService.
 */
@ExtendWith(MockitoExtension.class)
class SchemaMigrationServiceTest {

    private SchemaMigrationService migrationService;

    // Test migrators
    private TestMigrator migrator_0_to_1;
    private TestMigrator migrator_1_to_2;
    private TestMigrator migrator_2_to_3;

    @BeforeEach
    void setUp() {
        migrator_0_to_1 = new TestMigrator("TestEntity", "0", "1.0.0", "v1");
        migrator_1_to_2 = new TestMigrator("TestEntity", "1.0.0", "2.0.0", "v2");
        migrator_2_to_3 = new TestMigrator("TestEntity", "2.0.0", "3.0.0", "v3");

        List<SchemaMigrator> migrators = Arrays.asList(
                migrator_0_to_1,
                migrator_1_to_2,
                migrator_2_to_3
        );

        migrationService = new SchemaMigrationService(migrators);
    }

    @Test
    void shouldMigrateSingleStep() throws Exception {
        // Given
        String entityJson = "{\"id\":\"123\",\"name\":\"test\"}";

        // When
        String result = migrationService.migrate(entityJson, "TestEntity", SchemaVersion.of("1.0.0"));

        // Then
        assertThat(result).contains("\"migrated\":\"v1\"");
        assertThat(result).contains("\"_schema\":\"1.0.0\"");
    }

    @Test
    void shouldMigrateMultipleSteps() throws Exception {
        // Given
        String entityJson = "{\"id\":\"123\",\"name\":\"test\"}";

        // When
        String result = migrationService.migrate(entityJson, "TestEntity", SchemaVersion.of("3.0.0"));

        // Then
        assertThat(result).contains("\"migrated\":\"v1\"");
        assertThat(result).contains("\"migrated2\":\"v2\"");
        assertThat(result).contains("\"migrated3\":\"v3\"");
        assertThat(result).contains("\"_schema\":\"3.0.0\"");
    }

    @Test
    void shouldSkipMigrationWhenAlreadyAtTargetVersion() throws Exception {
        // Given
        String entityJson = "{\"id\":\"123\",\"name\":\"test\",\"_schema\":\"2.0.0\"}";

        // When
        String result = migrationService.migrate(entityJson, "TestEntity", SchemaVersion.of("2.0.0"));

        // Then
        assertThat(result).isEqualTo(entityJson);
        assertThat(result).doesNotContain("\"migrated\":\"v1\"");
    }

    @Test
    void shouldMigrateFromSpecificVersion() throws Exception {
        // Given - entity at version 1.0.0
        String entityJson = "{\"id\":\"123\",\"name\":\"test\",\"_schema\":\"1.0.0\"}";

        // When
        String result = migrationService.migrate(entityJson, "TestEntity", SchemaVersion.of("2.0.0"));

        // Then
        assertThat(result).contains("\"migrated2\":\"v2\"");
        assertThat(result).contains("\"_schema\":\"2.0.0\"");
        assertThat(result).doesNotContain("\"migrated\":\"v1\""); // Should not apply first migrator
    }

    @Test
    void shouldThrowExceptionWhenNoMigrationPathExists() {
        // Given
        String entityJson = "{\"id\":\"123\",\"name\":\"test\"}";

        // When/Then
        assertThatThrownBy(() -> migrationService.migrate(entityJson, "TestEntity", SchemaVersion.of("99.0.0")))
                .isInstanceOf(SchemaMigrationService.MigrationException.class)
                .hasMessageContaining("No migration path found");
    }

    @Test
    void shouldThrowExceptionWhenEntityTypeNotFound() {
        // Given
        String entityJson = "{\"id\":\"123\",\"name\":\"test\"}";

        // When/Then
        assertThatThrownBy(() -> migrationService.migrate(entityJson, "UnknownEntity", SchemaVersion.of("1.0.0")))
                .isInstanceOf(SchemaMigrationService.MigrationException.class)
                .hasMessageContaining("No migration path found");
    }

    @Test
    void shouldHandleEntityWithoutSchemaField() throws Exception {
        // Given - entity without _schema field (defaults to version "0")
        String entityJson = "{\"id\":\"123\",\"name\":\"test\"}";

        // When
        String result = migrationService.migrate(entityJson, "TestEntity", SchemaVersion.of("1.0.0"));

        // Then
        assertThat(result).contains("\"migrated\":\"v1\"");
        assertThat(result).contains("\"_schema\":\"1.0.0\"");
    }

    @Test
    void shouldCheckMigrationPathExists() {
        // When/Then
        assertThat(migrationService.hasMigrationPath("TestEntity", SchemaVersion.of("0"), SchemaVersion.of("3.0.0"))).isTrue();
        assertThat(migrationService.hasMigrationPath("TestEntity", SchemaVersion.of("1.0.0"), SchemaVersion.of("2.0.0"))).isTrue();
        assertThat(migrationService.hasMigrationPath("TestEntity", SchemaVersion.of("0"), SchemaVersion.of("99.0.0"))).isFalse();
        assertThat(migrationService.hasMigrationPath("UnknownEntity", SchemaVersion.of("0"), SchemaVersion.of("1.0.0"))).isFalse();
    }

    @Test
    void shouldReturnSameVersionWhenCheckingMigrationPath() {
        // When/Then
        assertThat(migrationService.hasMigrationPath("TestEntity", SchemaVersion.of("1.0.0"), SchemaVersion.of("1.0.0"))).isTrue();
    }

    @Test
    void shouldGetRegisteredEntityTypes() {
        // When
        var entityTypes = migrationService.getRegisteredEntityTypes();

        // Then
        assertThat(entityTypes).containsExactly("TestEntity");
    }

    @Test
    void shouldGetMigratorsForEntity() {
        // When
        List<SchemaMigrator> migrators = migrationService.getMigratorsForEntity("TestEntity");

        // Then
        assertThat(migrators).hasSize(3);
        assertThat(migrators).containsExactly(migrator_0_to_1, migrator_1_to_2, migrator_2_to_3);
    }

    @Test
    void shouldReturnEmptyListForUnknownEntity() {
        // When
        List<SchemaMigrator> migrators = migrationService.getMigratorsForEntity("UnknownEntity");

        // Then
        assertThat(migrators).isEmpty();
    }

    @Test
    void shouldHandleEmptyMigratorList() {
        // Given
        SchemaMigrationService emptyService = new SchemaMigrationService(Collections.emptyList());
        String entityJson = "{\"id\":\"123\",\"name\":\"test\"}";

        // When/Then
        assertThatThrownBy(() -> emptyService.migrate(entityJson, "TestEntity", SchemaVersion.of("1.0.0")))
                .isInstanceOf(SchemaMigrationService.MigrationException.class);
    }

    @Test
    void shouldPropagateExceptionFromMigrator() {
        // Given
        TestMigrator failingMigrator = new TestMigrator("TestEntity", "0", "1.0.0", "v1") {
            @Override
            public String migrate(String entityJson) {
                throw new RuntimeException("Migration failed");
            }
        };

        SchemaMigrationService service = new SchemaMigrationService(Collections.singletonList(failingMigrator));
        String entityJson = "{\"id\":\"123\",\"name\":\"test\"}";

        // When/Then
        assertThatThrownBy(() -> service.migrate(entityJson, "TestEntity", SchemaVersion.of("1.0.0")))
                .isInstanceOf(SchemaMigrationService.MigrationException.class)
                .hasMessageContaining("Migration failed");
    }

    /**
     * Test implementation of SchemaMigrator.
     */
    static class TestMigrator implements SchemaMigrator {
        private final String entityType;
        private final SchemaVersion fromVersion;
        private final SchemaVersion toVersion;
        private final String markerValue;

        TestMigrator(String entityType, String fromVersion, String toVersion, String markerValue) {
            this.entityType = entityType;
            this.fromVersion = SchemaVersion.of(fromVersion);
            this.toVersion = SchemaVersion.of(toVersion);
            this.markerValue = markerValue;
        }

        @Override
        public String getEntityType() {
            return entityType;
        }

        @Override
        public SchemaVersion getFromVersion() {
            return fromVersion;
        }

        @Override
        public SchemaVersion getToVersion() {
            return toVersion;
        }

        @Override
        public String migrate(String entityJson) {
            // Simple migration: add a marker field
            String trimmed = entityJson.trim();
            if (trimmed.endsWith("}")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
            }

            if (trimmed.length() > 1 && !trimmed.endsWith(",")) {
                trimmed += ",";
            }

            // Use different field names for different versions to test chaining
            String fieldName = switch (toVersion.toString()) {
                case "1.0.0" -> "migrated";
                case "2.0.0" -> "migrated2";
                case "3.0.0" -> "migrated3";
                default -> "migrated_" + toVersion;
            };

            return trimmed + "\"" + fieldName + "\":\"" + markerValue + "\"}";
        }
    }
}
