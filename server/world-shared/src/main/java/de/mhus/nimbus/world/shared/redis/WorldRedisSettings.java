package de.mhus.nimbus.world.shared.redis;

import de.mhus.nimbus.shared.service.SSettingsService;
import de.mhus.nimbus.shared.settings.SettingBoolean;
import de.mhus.nimbus.shared.settings.SettingInteger;
import de.mhus.nimbus.shared.settings.SettingString;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class WorldRedisSettings {

    private final SSettingsService settingsService;

    private SettingString host;
    private SettingInteger port;
    private SettingInteger database;
    private SettingString password;
    private SettingBoolean ssl;
    private SettingString redisUrl;

    @PostConstruct
    private void init() {
        redisUrl = settingsService.getString(
                "redis.url",
                null
        );

        // Parse Redis URL if provided
        String url = redisUrl.get();
        if (url != null && !url.isBlank()) {
            parseRedisUrl(url);
        } else {
            // Load individual settings
            host = settingsService.getString(
                    "redis.host",
                    "localhost"
            );
            port = settingsService.getInteger(
                    "redis.port",
                    6379
            );
            database = settingsService.getInteger(
                    "redis.database",
                    0
            );
            password = settingsService.getString(
                    "redis.password",
                    null
            );
            ssl = settingsService.getBoolean(
                    "redis.ssl",
                    false
            );
        }
    }

    private void parseRedisUrl(String url) {
        try {
            URI uri = URI.create(url);

            String parsedHost = uri.getHost() != null ? uri.getHost() : "localhost";
            int parsedPort = uri.getPort() > 0 ? uri.getPort() : 6379;
            int parsedDatabase = 0;
            String parsedPassword = null;
            boolean parsedSsl = false;

            if (uri.getUserInfo() != null) {
                String[] parts = uri.getUserInfo().split(":", 2);
                if (parts.length == 2) {
                    parsedPassword = parts[1];
                }
            }

            if (uri.getPath() != null && uri.getPath().length() > 1) {
                try {
                    parsedDatabase = Integer.parseInt(uri.getPath().substring(1));
                } catch (Exception ignored) {
                }
            }

            parsedSsl = uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("rediss");

            // Create settings with parsed values
            host = settingsService.getString("redis.host", parsedHost);
            port = settingsService.getInteger("redis.port", parsedPort);
            database = settingsService.getInteger("redis.database", parsedDatabase);
            password = settingsService.getString("redis.password", parsedPassword);
            ssl = settingsService.getBoolean("redis.ssl", parsedSsl);

        } catch (Exception e) {
            // Fallback to defaults on parse error
            host = settingsService.getString("redis.host", "localhost");
            port = settingsService.getInteger("redis.port", 6379);
            database = settingsService.getInteger("redis.database", 0);
            password = settingsService.getString("redis.password", null);
            ssl = settingsService.getBoolean("redis.ssl", false);
        }
    }

    public String getHost() {
        return host.get();
    }

    public int getPort() {
        return port.get();
    }

    public int getDatabase() {
        return database.get();
    }

    public String getPassword() {
        return password.get();
    }

    public boolean isSsl() {
        return ssl.get();
    }
}
