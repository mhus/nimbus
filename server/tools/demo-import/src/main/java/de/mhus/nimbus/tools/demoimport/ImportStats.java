package de.mhus.nimbus.tools.demoimport;

import lombok.Data;

/**
 * Tracks import statistics.
 */
@Data
public class ImportStats {
    private int totalCount;
    private int successCount;
    private int skippedCount;
    private int failureCount;

    public void incrementSuccess() {
        totalCount++;
        successCount++;
    }

    public void incrementSkipped() {
        totalCount++;
        skippedCount++;
    }

    public void incrementFailure() {
        totalCount++;
        failureCount++;
    }

    public void merge(ImportStats other) {
        this.totalCount += other.totalCount;
        this.successCount += other.successCount;
        this.skippedCount += other.skippedCount;
        this.failureCount += other.failureCount;
    }
}
