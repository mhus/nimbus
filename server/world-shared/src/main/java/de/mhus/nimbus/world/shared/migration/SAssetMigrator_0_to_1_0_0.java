package de.mhus.nimbus.world.shared.migration;

import de.mhus.nimbus.shared.persistence.SchemaMigrator;
import org.springframework.stereotype.Component;

/**
 * Schema migrator for SAsset from version 0 to 1.0.0.
 * This is the initial migration that sets the schema version without making changes.
 */
@Component
public class SAssetMigrator_0_to_1_0_0 implements SchemaMigrator {

    @Override
    public String getEntityType() {
        return "SAsset";
    }

    @Override
    public String getFromVersion() {
        return "0";
    }

    @Override
    public String getToVersion() {
        return "1.0.0";
    }

    @Override
    public String migrate(String entityJson) throws Exception {
        // No changes needed, just version upgrade
        return entityJson;
    }
}
