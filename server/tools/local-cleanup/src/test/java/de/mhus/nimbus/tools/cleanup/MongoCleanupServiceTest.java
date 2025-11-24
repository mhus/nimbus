package de.mhus.nimbus.tools.cleanup;

// Entfernt JUnit Imports wegen fehlender Auflösung in der aktuellen Umgebung.
// Einfacher Selbsttest via main. Bei echter JUnit Verfügbarkeit kann die Datei zurückgebaut werden.

public class MongoCleanupServiceTest {

    public static void main(String[] args) {
        MongoCleanupService svc = new MongoCleanupService("mongodb://localhost:27017", "universe, region ,world , ");
        if (svc == null) throw new IllegalStateException("Service ist null");
        var set = svc.getTargetDatabases();
        require(set.contains("universe"), "universe fehlt");
        require(set.contains("region"), "region fehlt");
        require(set.contains("world"), "world fehlt");
        require(set.size() == 3, "Erwartete Größe 3, ist " + set.size());
        System.out.println("MongoCleanupServiceTest: OK");
    }

    private static void require(boolean cond, String msg) {
        if (!cond) throw new IllegalStateException(msg);
    }
}
