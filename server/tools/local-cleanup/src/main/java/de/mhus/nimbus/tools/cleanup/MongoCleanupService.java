package de.mhus.nimbus.tools.cleanup;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class MongoCleanupService {

    private final String mongoUri;
    private final Set<String> targetDatabases;

    public MongoCleanupService(@Value("${cleanup.mongo.uri}") String mongoUri,
                               @Value("${cleanup.mongo.databases}") List<String> databasesCsv) {
        this.mongoUri = mongoUri;
        this.targetDatabases = new HashSet<>();
        databasesCsv.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(targetDatabases::add);
    }

    public Set<String> getTargetDatabases() { return new HashSet<>(targetDatabases); }

    public void cleanup() {
        log.info("Verbinde zu MongoDB (direkter Drop, kein Listing): {}", mongoUri);
        try (MongoClient client = MongoClients.create(MongoClientSettings.builder().applyConnectionString(new com.mongodb.ConnectionString(mongoUri)).build())) {
            for (String dbName : targetDatabases) {
                try {
                    log.warn("Dropping DB '{}'", dbName);
                    client.getDatabase(dbName).drop();
                    log.info("DB '{}' erfolgreich gedroppt", dbName);
                } catch (MongoCommandException mce) {
                    // Code 26: NamespaceNotFound (DB existiert nicht) – ignoriere
                    if (mce.getCode() == 26) {
                        log.info("DB '{}' existiert nicht – nichts zu löschen", dbName);
                    } else if (mce.getCode() == 13) { // Unauthorized
                        log.error("Keine Berechtigung zum Droppen von '{}' (Code 13). Prüfe Rollen oder Credentials.", dbName);
                        throw mce; // Abbrechen: sicherheitsrelevant
                    } else {
                        log.error("MongoCommandException beim Droppen '{}' (Code {}): {}", dbName, mce.getCode(), mce.getErrorMessage(), mce);
                        throw mce;
                    }
                } catch (Exception ex) {
                    log.error("Unerwarteter Fehler beim Droppen '{}': {}", dbName, ex.getMessage(), ex);
                    throw ex;
                }
            }
        } catch (Exception e) {
            log.error("Cleanup insgesamt fehlgeschlagen: {}", e.getMessage());
            throw e;
        }
    }
}
