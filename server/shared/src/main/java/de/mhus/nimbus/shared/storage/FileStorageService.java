package de.mhus.nimbus.shared.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class FileStorageService implements StorageService {

    @Value("${nimbus.storage.path:target/data}")
    private String basePath;

    @Override
    public String store(String path, byte[] data) {
        path = normalizePath(path);
        storeToFile(path, data);
        return path;
    }

    private void storeToFile(String path, byte[] data) {
        var file = new File(basePath, path);
        file.getParentFile().mkdirs();
        try {
            java.nio.file.Files.write(file.toPath(), data);
        } catch (Exception e) {
            log.error("Error storing file to " + file.getAbsolutePath(), e);
        }
    }

    private String normalizePath(String path) {
        return path
                .replace("..", "")
                .replace("\\", "/")
                .replace("//", "/")
                .replace("~", "_");
    }

    @Override
    public byte[] load(String storageId) {
        storageId = normalizePath(storageId);
        var file = new File(basePath, storageId);
        if (!file.exists()) return null;
        try {
            return java.nio.file.Files.readAllBytes(file.toPath());
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
    public String update(String storageId, byte[] data) {
        storageId = normalizePath(storageId);
        var file = new File(basePath, storageId);
        if (!file.exists()) return null;
        storeToFile(storageId, data);
        return storageId;
    }

}
