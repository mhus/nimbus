package de.mhus.nimbus.shared.storage;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Abstraktion eines externen Speichers (aktuell Dateisystem). Große Assets werden hier gespeichert.
 */
public abstract class StorageService {
    /** Speichert Daten und liefert eine Storage-Id. */
    public abstract StorageInfo store(String worldId, String path, InputStream stream);
    /** Lädt Daten anhand der Storage-Id. */
    public abstract InputStream load(String storageId);
    /** Entfernt abgelegten Inhalt. */
    public abstract void delete(String storageId);
    public abstract StorageInfo update(String storageId, InputStream stream);

    public abstract StorageInfo info(String storageId);

    public record StorageInfo(String id, long size, Date createdAt, String worldId, String path) { }

}

