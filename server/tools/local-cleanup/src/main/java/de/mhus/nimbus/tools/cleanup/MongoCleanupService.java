package de.mhus.nimbus.tools.cleanup;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class MongoCleanupService {

    private static final Logger LOG = LoggerFactory.getLogger(MongoCleanupService.class);

    private final String mongoUri;
    private final Set<String> targetDatabases;

    public MongoCleanupService(@Value("${cleanup.mongo.uri:mongodb://localhost:27017}") String mongoUri,
                               @Value("${cleanup.mongo.databases:universe,region,world}") String databasesCsv) {
        this.mongoUri = mongoUri;
        this.targetDatabases = new HashSet<>();
        Arrays.stream(databasesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(targetDatabases::add);
    }

    public Set<String> getTargetDatabases() { return new HashSet<>(targetDatabases); }

    public void cleanup() {
        LOG.info("Verbinde zu MongoDB: {}", mongoUri);
        try (MongoClient client = MongoClients.create(MongoClientSettings.builder().applyConnectionString(new com.mongodb.ConnectionString(mongoUri)).build())) {
            MongoIterable<String> dbs = client.listDatabaseNames();
            for (String dbName : dbs) {
                if (targetDatabases.contains(dbName)) {
                    LOG.warn("Dropping DB '{}'", dbName);
                    client.getDatabase(dbName).drop();
                } else {
                    LOG.debug("Überspringe DB '{}'", dbName);
                }
            }
        }
    }
}
