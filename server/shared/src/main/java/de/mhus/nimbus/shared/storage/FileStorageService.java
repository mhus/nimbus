package de.mhus.nimbus.shared.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.util.Date;

@Service
@Slf4j
public class FileStorageService extends StorageService {

    @Value("${nimbus.storage.path:data}")
    private String basePath;

    @Override
    public StorageInfo store(String path, InputStream stream) {
        path = normalizePath(path);
        return storeToFile(path, stream);
    }

    private StorageInfo storeToFile(String path, InputStream stream) {
        var file = new File(basePath, path);
        file.getParentFile().mkdirs();
        try {
            var size = java.nio.file.Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return new StorageInfo(
                    path,
                    size,
                    new Date(),
                    path
            );
        } catch (Exception e) {
            log.error("Error storing file to " + file.getAbsolutePath(), e);
        }
        return null;
    }

    private String normalizePath(String path) {
        return path
                .replace("..", "")
                .replace("\\", "/")
                .replace("//", "/")
                .replace("~", "_");
    }

    @Override
    public InputStream load(String storageId) {
        storageId = normalizePath(storageId);
        var file = new File(basePath, storageId);
        if (!file.exists()) return null;
        try {
            return new java.io.FileInputStream(file);
        } catch (Exception e) {
            log.error("Error loading file from " + file.getAbsolutePath(), e);
        }
        return null;
    }

    @Override
    public void delete(String storageId) {
        storageId = normalizePath(storageId);
        var file = new File(basePath, storageId);
        if (!file.exists()) return;
        file.delete();
    }

    @Override
    public StorageInfo update(String storageId, InputStream stream) {
        storageId = normalizePath(storageId);
        var file = new File(basePath, storageId);
        if (!file.exists()) return null;
        storeToFile(storageId, stream);
        return new StorageInfo(
                storageId,
                file.length(),
                new Date(file.lastModified()),
                storageId
        );
    }

    @Override
    public StorageInfo info(String storageId) {
        storageId = normalizePath(storageId);
        var file = new File(basePath, storageId);
        if (!file.exists()) return null;
        return new StorageInfo(
                storageId,
                file.length(),
                new Date(file.lastModified()),
                storageId
        );
    }

}
