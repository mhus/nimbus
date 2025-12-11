package de.mhus.nimbus.shared.config;

import de.mhus.nimbus.shared.persistence.ActualSchemaVersion;
import de.mhus.nimbus.shared.service.SchemaMigrationService;
import de.mhus.nimbus.shared.types.Identifiable;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterConvertEvent;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB event listener that automatically manages schema versioning for entities.
 *
 * <p>This listener performs two main functions:</p>
 * <ul>
 *   <li><b>Before Save:</b> Automatically adds the "_schema" field to MongoDB documents
 *       based on the {@link ActualSchemaVersion} annotation on the entity class.</li>
 *   <li><b>After Load:</b> Validates that the loaded document's schema version matches
 *       the expected version. Can optionally trigger automatic migration.</li>
 * </ul>
 *
 * <p>Performance optimization: Schema versions are cached in memory to avoid
 * repeated reflection calls on entity classes.</p>
 *
 * <p>Configuration:</p>
 * <ul>
 *   <li>{@code nimbus.schema.auto-migrate=true}: Enable automatic migration on load (default: false)</li>
 * </ul>
 */
@Component
@Slf4j
public class SchemaVersionEventListener extends AbstractMongoEventListener<Object> {

    private final ConcurrentHashMap<Class<?>, String> schemaVersionCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private SchemaMigrationService migrationService;

    @Value("${nimbus.schema.auto-migrate:false}")
    private boolean autoMigrate;

    /**
     * Called before an entity is converted to a MongoDB document for saving.
     * Adds the "_schema" field with the version from the {@link ActualSchemaVersion} annotation.
     *
     * @param event the before convert event containing the entity and document
     */
    @Override
    public void onBeforeConvert(BeforeConvertEvent<Object> event) {
        Object source = event.getSource();
        Document document = event.getDocument();

        if (document == null || source == null) {
            return;
        }

        // Extract schema version from annotation (with caching for performance)
        Class<?> entityClass = source.getClass();
        String version = getSchemaVersion(entityClass);

        if (version != null) {
            document.put("_schema", version);

            log.trace("Added _schema={} to {} in collection {}",
                      version,
                      source.getClass().getSimpleName(),
                      event.getCollectionName());
        }
    }

    /**
     * Called after a MongoDB document is converted to an entity.
     * Validates that the document's schema version matches the expected version.
     *
     * @param event the after convert event containing the entity and document
     */
    @Override
    public void onAfterConvert(AfterConvertEvent<Object> event) {
        Object entity = event.getSource();
        Document document = event.getDocument();

        if (entity == null || document == null) {
            return;
        }

        // Get schema version from document and expected version from annotation
        String documentSchema = document.getString("_schema");
        Class<?> entityClass = entity.getClass();
        String expectedVersion = getSchemaVersion(entityClass);

        // Check if schema version doesn't match
        if (expectedVersion != null && !expectedVersion.equals(documentSchema)) {
            handleSchemaMismatch(entity, documentSchema, expectedVersion);
        }
    }

    /**
     * Gets the schema version for an entity class from its {@link ActualSchemaVersion} annotation.
     * Results are cached for performance.
     *
     * @param entityClass the entity class to get the schema version for
     * @return the schema version string, or null if no annotation is present
     */
    private String getSchemaVersion(Class<?> entityClass) {
        return schemaVersionCache.computeIfAbsent(entityClass, clazz -> {
            ActualSchemaVersion annotation = clazz.getAnnotation(ActualSchemaVersion.class);
            return annotation != null ? annotation.value() : null;
        });
    }

    /**
     * Handles the case when a loaded document's schema version doesn't match
     * the expected version from the entity class annotation.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Always logs a warning about the mismatch</li>
     *   <li>If {@code nimbus.schema.auto-migrate=true} and migration service is available,
     *       attempts automatic migration</li>
     *   <li>Migration happens in-place on the loaded entity (changes are not automatically persisted)</li>
     * </ul>
     *
     * @param entity the loaded entity instance
     * @param documentSchema the schema version from the MongoDB document (may be null)
     * @param expectedSchema the expected schema version from the annotation
     */
    private void handleSchemaMismatch(Object entity, String documentSchema, String expectedSchema) {
        String entityType = entity.getClass().getSimpleName();
        String currentVersion = documentSchema != null ? documentSchema : "0";
        if (entity instanceof Identifiable identifiable) {
            entityType += " (ID: " + identifiable.getId() + ")";
        }

        log.warn("Schema version mismatch for entity {}: document has schema '{}', expected '{}'",
                 entityType,
                 currentVersion,
                 expectedSchema);

        // Automatic migration if enabled
        if (autoMigrate && migrationService != null) {
            try {
                log.info("Attempting automatic migration for {} from {} to {}",
                        entityType, currentVersion, expectedSchema);

                // Note: This logs the attempt but actual migration would require
                // access to the raw document, which is not available in AfterConvertEvent.
                // For real automatic migration, use the migration command or service directly.

                log.warn("Automatic migration during load is not yet implemented. " +
                        "Please use the migration command to migrate documents manually.");

            } catch (Exception e) {
                log.error("Automatic migration failed for {} from {} to {}: {}",
                        entityType, currentVersion, expectedSchema, e.getMessage());
            }
        } else if (autoMigrate) {
            log.debug("Auto-migrate enabled but migration service not available");
        }
    }
}
