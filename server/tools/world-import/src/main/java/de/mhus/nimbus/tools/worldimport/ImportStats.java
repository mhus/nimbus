package de.mhus.nimbus.tools.worldimport;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Statistics for import operations.
 */
@Data
public class ImportStats {
    private int totalCollections = 0;
    private int successCount = 0;
    private int failureCount = 0;
    private int totalEntities = 0;
    private int totalMigrated = 0;
    private long durationMs = 0;
    private List<String> failedCollections = new ArrayList<>();

    public void incrementSuccess(int entityCount, int migratedCount) {
        successCount++;
        totalEntities += entityCount;
        totalMigrated += migratedCount;
    }

    public void incrementFailure(String collectionName) {
        failureCount++;
        failedCollections.add(collectionName);
    }

    public boolean hasErrors() {
        return failureCount > 0;
    }
}
