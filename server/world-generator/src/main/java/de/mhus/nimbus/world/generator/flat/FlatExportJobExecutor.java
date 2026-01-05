package de.mhus.nimbus.world.generator.flat;

import de.mhus.nimbus.world.shared.generator.WFlatService;
import de.mhus.nimbus.world.shared.job.JobExecutionException;
import de.mhus.nimbus.world.shared.job.JobExecutor;
import de.mhus.nimbus.world.shared.job.WJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Job executor for exporting WFlat to WLayer GROUND type.
 * Writes flat terrain data back to layer chunks.
 *
 * WorldId is taken from job.getWorldId()
 *
 * Required parameters:
 * - flatId: Database ID of the WFlat to export
 * - layerName: Name of the target GROUND layer
 *
 * Optional parameters:
 * - deleteAfterExport: If true, deletes the WFlat after successful export (default: false)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FlatExportJobExecutor implements JobExecutor {

    private static final String EXECUTOR_NAME = "flat-export";

    private final FlatExportService flatExportService;
    private final WFlatService flatService;

    @Override
    public String getExecutorName() {
        return EXECUTOR_NAME;
    }

    @Override
    public JobResult execute(WJob job) throws JobExecutionException {
        try {
            log.info("Starting flat export job: jobId={}", job.getId());

            // Get worldId from job
            String worldId = job.getWorldId();

            // Extract and validate required parameters
            String flatId = getRequiredParameter(job, "flatId");
            String layerName = getRequiredParameter(job, "layerName");

            // Extract optional parameter
            boolean deleteAfterExport = getOptionalBooleanParameter(job, "deleteAfterExport", false);

            log.info("Exporting flat: flatId={}, worldId={}, layerName={}, deleteAfterExport={}",
                    flatId, worldId, layerName, deleteAfterExport);

            // Execute export
            int exportedColumns = flatExportService.exportToLayer(flatId, worldId, layerName);

            // Delete flat if requested
            if (deleteAfterExport) {
                log.info("Deleting flat after export: flatId={}", flatId);
                flatService.deleteById(flatId);
                log.info("Flat deleted: flatId={}", flatId);
            }

            // Build success result
            String resultData = String.format(
                    "Successfully exported flat: flatId=%s, worldId=%s, layerName=%s, exportedColumns=%d, deleted=%s",
                    flatId, worldId, layerName, exportedColumns, deleteAfterExport
            );

            log.info("Flat export completed successfully: flatId={}, exportedColumns={}, deleted={}",
                    flatId, exportedColumns, deleteAfterExport);
            return JobResult.ofSuccess(resultData);

        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for flat export", e);
            throw new JobExecutionException("Invalid parameters: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Flat export failed", e);
            throw new JobExecutionException("Export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get required string parameter from job.
     */
    private String getRequiredParameter(WJob job, String paramName) throws JobExecutionException {
        String value = job.getParameters().get(paramName);
        if (value == null || value.isBlank()) {
            throw new JobExecutionException("Missing required parameter: " + paramName);
        }
        return value;
    }

    /**
     * Get optional boolean parameter from job with default value.
     */
    private boolean getOptionalBooleanParameter(WJob job, String paramName, boolean defaultValue) {
        String value = job.getParameters().get(paramName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        // Parse boolean (true, false, 1, 0, yes, no)
        return "true".equalsIgnoreCase(value)
            || "1".equals(value)
            || "yes".equalsIgnoreCase(value);
    }
}
