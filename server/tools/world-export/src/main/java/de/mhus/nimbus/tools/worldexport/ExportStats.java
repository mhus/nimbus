package de.mhus.nimbus.tools.worldexport;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Statistics for export operations.
 */
@Data
public class ExportStats {
    private int totalCollections = 0;
    private int successCount = 0;
    private int failureCount = 0;
    private int totalEntities = 0;
    private long durationMs = 0;
    private List<String> failedCollections = new ArrayList<>();

    public void incrementSuccess(int entityCount) {
        successCount++;
        totalEntities += entityCount;
    }

    public void incrementFailure(String collectionName) {
        failureCount++;
        failedCollections.add(collectionName);
    }

    public boolean hasErrors() {
        return failureCount > 0;
    }
}
