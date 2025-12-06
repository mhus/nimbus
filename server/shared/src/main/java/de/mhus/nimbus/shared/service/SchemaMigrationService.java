package de.mhus.nimbus.shared.service;

import de.mhus.nimbus.shared.persistence.SchemaMigrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing and executing schema migrations on MongoDB entities.
 *
 * <p>This service automatically discovers all {@link SchemaMigrator} beans
 * and uses them to migrate entities from their current version to the latest version.</p>
 *
 * <p>Migration process:</p>
 * <ol>
 *   <li>Determine current version from entity's {@code _schema} field (or "0" if missing)</li>
 *   <li>Find migration path from current version to target version</li>
 *   <li>Apply migrations sequentially</li>
 *   <li>Update {@code _schema} field to target version</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>
 * String migratedJson = schemaMigrationService.migrate(entityJson, "UUser", "2.0.0");
 * </pre>
 */
@Service
@Slf4j
public class SchemaMigrationService {

    private final List<SchemaMigrator> migrators;
    private final Map<String, List<SchemaMigrator>> migratorsByEntity = new HashMap<>();

    /**
     * Constructor with lazy injection of all available migrators.
     *
     * @param migrators list of all registered SchemaMigrator beans
     */
    public SchemaMigrationService(List<SchemaMigrator> migrators) {
        this.migrators = migrators;
        initializeMigratorCache();
    }

    /**
     * Initializes the migrator cache, organizing migrators by entity type
     * and sorting them by version order.
     */
    private void initializeMigratorCache() {
        for (SchemaMigrator migrator : migrators) {
            String entityType = migrator.getEntityType();
            migratorsByEntity
                    .computeIfAbsent(entityType, k -> new ArrayList<>())
                    .add(migrator);

            log.debug("Registered migrator for {}: {} -> {}",
                    entityType,
                    migrator.getFromVersion(),
                    migrator.getToVersion());
        }

        // Sort migrators by version order for each entity type
        for (List<SchemaMigrator> entityMigrators : migratorsByEntity.values()) {
            entityMigrators.sort(Comparator.comparing(SchemaMigrator::getFromVersion));
        }

        log.info("Initialized schema migration service with {} migrators for {} entity types",
                migrators.size(),
                migratorsByEntity.size());
    }

    /**
     * Migrates an entity from its current version to the target version.
     *
     * @param entityJson  the entity as JSON string (MongoDB document)
     * @param entityType  the entity type name (e.g., "UUser", "WWorld")
     * @param targetVersion the target schema version
     * @return the migrated entity as JSON string
     * @throws MigrationException if migration fails or no migration path exists
     */
    public String migrate(String entityJson, String entityType, String targetVersion) throws MigrationException {
        String currentVersion = extractSchemaVersion(entityJson);

        log.debug("Migrating {} from version {} to {}",
                entityType, currentVersion, targetVersion);

        // Check if migration is needed
        if (currentVersion.equals(targetVersion)) {
            log.trace("Entity {} already at target version {}", entityType, targetVersion);
            return entityJson;
        }

        // Find migration path
        List<SchemaMigrator> migrationPath = findMigrationPath(entityType, currentVersion, targetVersion);

        if (migrationPath.isEmpty() && !currentVersion.equals(targetVersion)) {
            throw new MigrationException(String.format(
                    "No migration path found for %s from version %s to %s",
                    entityType, currentVersion, targetVersion));
        }

        // Apply migrations sequentially
        String result = entityJson;
        for (SchemaMigrator migrator : migrationPath) {
            try {
                log.debug("Applying migration {} -> {} for {}",
                        migrator.getFromVersion(),
                        migrator.getToVersion(),
                        entityType);

                result = migrator.migrate(result);
                result = updateSchemaVersion(result, migrator.getToVersion());

            } catch (Exception e) {
                throw new MigrationException(String.format(
                        "Migration failed for %s from %s to %s: %s",
                        entityType,
                        migrator.getFromVersion(),
                        migrator.getToVersion(),
                        e.getMessage()), e);
            }
        }

        log.info("Successfully migrated {} from version {} to {}",
                entityType, currentVersion, targetVersion);

        return result;
    }

    /**
     * Finds the migration path from source version to target version.
     *
     * @param entityType the entity type
     * @param fromVersion the source version
     * @param toVersion the target version
     * @return ordered list of migrators to apply
     */
    private List<SchemaMigrator> findMigrationPath(String entityType, String fromVersion, String toVersion) {
        List<SchemaMigrator> entityMigrators = migratorsByEntity.getOrDefault(entityType, Collections.emptyList());

        List<SchemaMigrator> path = new ArrayList<>();
        String currentVersion = fromVersion;

        // Build migration chain
        for (SchemaMigrator migrator : entityMigrators) {
            if (migrator.getFromVersion().equals(currentVersion)) {
                path.add(migrator);
                currentVersion = migrator.getToVersion();

                // Stop if we reached target version
                if (currentVersion.equals(toVersion)) {
                    break;
                }
            }
        }

        // Verify we reached the target version
        if (!currentVersion.equals(toVersion) && !path.isEmpty()) {
            log.warn("Migration path for {} incomplete: reached {} instead of {}",
                    entityType, currentVersion, toVersion);
            return Collections.emptyList();
        }

        return path;
    }

    /**
     * Extracts the schema version from an entity JSON string.
     * Returns "0" if no _schema field is present.
     *
     * @param entityJson the entity as JSON string
     * @return the schema version, or "0" if not present
     */
    private String extractSchemaVersion(String entityJson) {
        // Simple JSON parsing to extract _schema field
        // Format: "\"_schema\":\"1.0.0\""
        int schemaIndex = entityJson.indexOf("\"_schema\"");
        if (schemaIndex == -1) {
            return "0";
        }

        int valueStart = entityJson.indexOf("\"", schemaIndex + 10);
        if (valueStart == -1) {
            return "0";
        }

        int valueEnd = entityJson.indexOf("\"", valueStart + 1);
        if (valueEnd == -1) {
            return "0";
        }

        return entityJson.substring(valueStart + 1, valueEnd);
    }

    /**
     * Updates the _schema field in an entity JSON string.
     *
     * @param entityJson the entity as JSON string
     * @param newVersion the new schema version
     * @return the entity JSON with updated _schema field
     */
    private String updateSchemaVersion(String entityJson, String newVersion) {
        // Remove trailing } to add/update _schema field
        String trimmed = entityJson.trim();
        if (trimmed.endsWith("}")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }

        // Remove existing _schema field if present
        int schemaIndex = trimmed.indexOf("\"_schema\"");
        if (schemaIndex != -1) {
            // Find the end of the _schema field (next comma or end)
            int fieldEnd = trimmed.indexOf(",", schemaIndex);
            if (fieldEnd == -1) {
                fieldEnd = trimmed.length();
            } else {
                fieldEnd++; // Include the comma
            }
            trimmed = trimmed.substring(0, schemaIndex) + trimmed.substring(fieldEnd);
        }

        // Add comma if there are other fields
        if (trimmed.trim().length() > 1 && !trimmed.trim().endsWith(",")) {
            trimmed += ",";
        }

        // Add _schema field
        return trimmed + "\"_schema\":\"" + newVersion + "\"}";
    }

    /**
     * Checks if a migration path exists for the given entity type and version range.
     *
     * @param entityType the entity type
     * @param fromVersion the source version
     * @param toVersion the target version
     * @return true if a migration path exists
     */
    public boolean hasMigrationPath(String entityType, String fromVersion, String toVersion) {
        if (fromVersion.equals(toVersion)) {
            return true;
        }
        List<SchemaMigrator> path = findMigrationPath(entityType, fromVersion, toVersion);
        return !path.isEmpty();
    }

    /**
     * Returns all registered entity types that have migrators.
     *
     * @return set of entity type names
     */
    public Set<String> getRegisteredEntityTypes() {
        return new HashSet<>(migratorsByEntity.keySet());
    }

    /**
     * Returns all migrators for a specific entity type.
     *
     * @param entityType the entity type
     * @return list of migrators, or empty list if none exist
     */
    public List<SchemaMigrator> getMigratorsForEntity(String entityType) {
        return migratorsByEntity.getOrDefault(entityType, Collections.emptyList());
    }

    /**
     * Returns the latest (highest) schema version available for an entity type.
     * Returns "0" if no migrators exist for the entity type.
     *
     * @param entityType the entity type
     * @return the latest schema version
     */
    public String getLatestVersion(String entityType) {
        List<SchemaMigrator> entityMigrators = migratorsByEntity.getOrDefault(entityType, Collections.emptyList());

        if (entityMigrators.isEmpty()) {
            return "0";
        }

        // Find the highest toVersion from all migrators
        return entityMigrators.stream()
                .map(SchemaMigrator::getToVersion)
                .max(String::compareTo)
                .orElse("0");
    }

    /**
     * Migrates an entity to the latest available version.
     *
     * @param entityJson the entity as JSON string
     * @param entityType the entity type name
     * @return the migrated entity as JSON string
     * @throws MigrationException if migration fails
     */
    public String migrateToLatest(String entityJson, String entityType) throws MigrationException {
        String latestVersion = getLatestVersion(entityType);
        return migrate(entityJson, entityType, latestVersion);
    }

    /**
     * Exception thrown when schema migration fails.
     */
    public static class MigrationException extends Exception {
        public MigrationException(String message) {
            super(message);
        }

        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
