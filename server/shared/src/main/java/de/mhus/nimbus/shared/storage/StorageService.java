package de.mhus.nimbus.shared.storage;

/**
 * Abstraktion eines externen Speichers (aktuell Dateisystem). Große Assets werden hier gespeichert.
 */
public interface StorageService {
    /** Speichert Daten und liefert eine Storage-Id. */
    String store(String path, byte[] data);
    /** Lädt Daten anhand der Storage-Id. */
    byte[] load(String storageId);
    /** Entfernt abgelegten Inhalt. */
    void delete(String storageId);
    String update(String storageId, byte[] data);
}

