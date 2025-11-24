package de.mhus.nimbus.shared.asset;

/**
 * Abstraktion eines externen Speichers (aktuell Dateisystem). Große Assets werden hier gespeichert.
 */
public interface StorageService {
    /** Speichert Daten und liefert eine Storage-Id. */
    String store(byte[] data);
    /** Lädt Daten anhand der Storage-Id. */
    byte[] load(String storageId);
    /** Entfernt abgelegten Inhalt. */
    void delete(String storageId);
}

