package de.mhus.nimbus.shared.config;

import de.mhus.nimbus.shared.persistence.SchemaVersion;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
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
 *       based on the {@link SchemaVersion} annotation on the entity class.</li>
 *   <li><b>After Load:</b> Validates that the loaded document's schema version matches
 *       the expected version. Logs a warning if there's a mismatch.</li>
 * </ul>
 *
 * <p>Performance optimization: Schema versions are cached in memory to avoid
 * repeated reflection calls on entity classes.</p>
 */
@Component
@Slf4j
public class SchemaVersionEventListener extends AbstractMongoEventListener<Object> {

    private final ConcurrentHashMap<Class<?>, String> schemaVersionCache = new ConcurrentHashMap<>();

    /**
     * Called before an entity is converted to a MongoDB document for saving.
     * Adds the "_schema" field with the version from the {@link SchemaVersion} annotation.
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
     * Gets the schema version for an entity class from its {@link SchemaVersion} annotation.
     * Results are cached for performance.
     *
     * @param entityClass the entity class to get the schema version for
     * @return the schema version string, or null if no annotation is present
     */
    private String getSchemaVersion(Class<?> entityClass) {
        return schemaVersionCache.computeIfAbsent(entityClass, clazz -> {
            SchemaVersion annotation = clazz.getAnnotation(SchemaVersion.class);
            return annotation != null ? annotation.value() : null;
        });
    }

    /**
     * Handles the case when a loaded document's schema version doesn't match
     * the expected version from the entity class annotation.
     *
     * <p>Currently logs a warning. Can be extended in the future to:</p>
     * <ul>
     *   <li>Call a migration service to automatically update the entity</li>
     *   <li>Throw an exception for incompatible schema versions</li>
     *   <li>Track metrics for monitoring schema version distribution</li>
     * </ul>
     *
     * @param entity the loaded entity instance
     * @param documentSchema the schema version from the MongoDB document (may be null)
     * @param expectedSchema the expected schema version from the annotation
     */
    private void handleSchemaMismatch(Object entity, String documentSchema, String expectedSchema) {
        log.warn("Schema version mismatch for entity {}: document has schema '{}', expected '{}'",
                 entity.getClass().getSimpleName(),
                 documentSchema != null ? documentSchema : "<none>",
                 expectedSchema);

        // Future extension point: call migration service
        // e.g.: schemaMigrationService.migrate(entity, documentSchema, expectedSchema);
    }
}
