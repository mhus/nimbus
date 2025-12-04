package de.mhus.nimbus.shared.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

/**
 * MongoDB-based storage service with automatic chunking support.
 *
 * Implementation strategy:
 * - Files are split into configurable chunks (default 512KB)
 * - Each chunk is stored as a separate MongoDB document
 * - UUID identifies file version across all chunks
 * - Soft-delete with 5-minute delay for safe cleanup
 * - Stream-based API for memory-efficient large file handling
 *
 * Memory usage: O(chunk-size) regardless of file size
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MongoStorageService extends StorageService {

    private final StorageDataRepository storageDataRepository;
    private final StorageDeleteRepository storageDeleteRepository;

    @Value("${nimbus.storage.chunk-size:524288}")
    private int chunkSize; // 512KB default

    @Override
    @Transactional
    public StorageInfo store(String path, InputStream stream) {
        if (stream == null) {
            log.error("Cannot store null stream for path: {}", path);
            return null;
        }

        String uuid = UUID.randomUUID().toString();
        Date createdAt = new Date();

        try (ChunkedOutputStream outputStream = new ChunkedOutputStream(
                storageDataRepository, uuid, path, chunkSize, createdAt)) {

            // Copy from input stream to chunked output stream
            // ChunkedOutputStream automatically splits into chunks and saves to MongoDB
            stream.transferTo(outputStream);

            long totalSize = outputStream.getTotalBytesWritten();

            log.debug("Stored file: uuid={} path={} size={}", uuid, path, totalSize);

            return new StorageInfo(uuid, totalSize, createdAt, path);

        } catch (IOException e) {
            log.error("Error storing file: path={}", path, e);
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public InputStream load(String storageId) {
        if (storageId == null || storageId.isBlank()) {
            log.error("Cannot load with null/empty storageId");
            return null;
        }

        try {
            // ChunkedInputStream automatically loads chunks one at a time on-demand
            return new ChunkedInputStream(storageDataRepository, storageId);

        } catch (Exception e) {
            log.error("Error loading storageId: {}", storageId, e);
            return null;
        }
    }

    @Override
    @Transactional
    public void delete(String storageId) {
        if (storageId == null || storageId.isBlank()) {
            log.error("Cannot delete with null/empty storageId");
            return;
        }

        // Schedule deletion in 5 minutes (soft-delete)
        // This allows ongoing read operations to complete safely
        Date deletedAt = new Date(System.currentTimeMillis() + 5 * 60 * 1000);

        StorageDelete deleteEntry = StorageDelete.builder()
                .storageId(storageId)
                .deletedAt(deletedAt)
                .build();

        storageDeleteRepository.save(deleteEntry);

        log.debug("Scheduled deletion: storageId={} at={}", storageId, deletedAt);
    }

    @Override
    @Transactional
    public StorageInfo update(String storageId, InputStream stream) {
        if (storageId == null || storageId.isBlank()) {
            log.error("Cannot update with null/empty storageId");
            return null;
        }

        if (stream == null) {
            log.error("Cannot update with null stream for storageId: {}", storageId);
            return null;
        }

        // Get old metadata for path reference
        StorageData oldFinalChunk = storageDataRepository.findByUuidAndIsFinalTrue(storageId);
        String path = (oldFinalChunk != null) ? oldFinalChunk.getPath() : "unknown";

        // Store new version with new UUID
        StorageInfo newInfo = store(path, stream);

        // Schedule old version for deletion
        if (newInfo != null) {
            delete(storageId);
            log.debug("Updated storage: oldId={} newId={}", storageId, newInfo.id());
        }

        return newInfo;
    }

    @Override
    public StorageInfo info(String storageId) {
        if (storageId == null || storageId.isBlank()) {
            log.error("Cannot get info with null/empty storageId");
            return null;
        }

        StorageData finalChunk = storageDataRepository.findByUuidAndIsFinalTrue(storageId);

        if (finalChunk == null) {
            log.warn("No final chunk found for storageId: {}", storageId);
            return null;
        }

        return new StorageInfo(
                storageId,
                finalChunk.getSize(),
                finalChunk.getCreatedAt(),
                finalChunk.getPath()
        );
    }
}
