package de.mhus.nimbus.world.shared.session;

import de.mhus.nimbus.shared.types.PlayerId;
import de.mhus.nimbus.shared.types.WorldId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WSessionService {

    private final StringRedisTemplate redis;
    private final WorldProperties props;

    private static final String KEY_PREFIX = "wsession:"; // Namespace
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_WORLD = "world";
    private static final String FIELD_USER = "user";
    private static final String FIELD_PLAYER_URL = "playerUrl";
    private static final String FIELD_CREATED = "created";
    private static final String FIELD_UPDATED = "updated";
    private static final String FIELD_EXPIRE = "expire";

    private static final String ID_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ID_LENGTH = 60;

    public WSession create(WorldId worldId, PlayerId playerId) {
        String id = randomId();
        Instant now = Instant.now();
        Duration effectiveTtl = Duration.ofMinutes(props.getWaitingMinutes());
        Instant expire = now.plus(effectiveTtl);
        WSession session = WSession.builder()
                .id(id)
                .status(WSessionStatus.WAITING)
                .worldId(worldId.getId())
                .playerId(playerId.getId())
                .createdAt(now)
                .updatedAt(now)
                .expireAt(expire)
                .build();
        write(session, effectiveTtl);
        log.debug("WSession erstellt id={} world={} user={} status=WAITING ttl={}min", id, worldId, playerId, effectiveTtl.toMinutes());
        return session;
    }

    public Optional<WSession> get(String id) {
        var ops = redis.opsForHash();
        var map = ops.entries(key(id));
        if (map == null || map.isEmpty()) return Optional.empty();
        try {
            WSession session = WSession.builder()
                    .id(id)
                    .status(WSessionStatus.valueOf((String) map.get(FIELD_STATUS)))
                    .worldId((String) map.get(FIELD_WORLD))
                    .playerId((String) map.get(FIELD_USER))
                    .playerUrl((String) map.get(FIELD_PLAYER_URL))
                    .createdAt(Instant.parse((String) map.get(FIELD_CREATED)))
                    .updatedAt(Instant.parse((String) map.get(FIELD_UPDATED)))
                    .expireAt(Instant.parse((String) map.get(FIELD_EXPIRE)))
                    .build();
            return Optional.of(session);
        } catch (Exception e) {
            log.warn("Fehler beim Lesen der Session {}: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Lädt eine WSession aus Redis mit allen Daten inklusive internal player URL.
     * Diese Methode ist identisch zu get(), aber mit aussagekräftigerem Namen für
     * den Use-Case, wenn explizit die Player-URL benötigt wird.
     *
     * @param sessionId Die Session-ID
     * @return Optional mit WSession inkl. playerUrl, oder empty wenn nicht gefunden
     */
    public Optional<WSession> getWithPlayerUrl(String sessionId) {
        return get(sessionId);
    }

    public Optional<WSession> updateStatus(String id, WSessionStatus newStatus) {
        return get(id).map(existing -> {
            existing.setStatus(newStatus);
            existing.touchUpdate();
            Duration newTtl = switch (newStatus) {
                case WAITING -> Duration.ofMinutes(props.getWaitingMinutes());
                case RUNNING -> Duration.ofHours(props.getRunningHours());
                case CLOSED -> Duration.ofMinutes(props.getDeprecatedMinutes());
            };
            existing.setExpireAt(Instant.now().plus(newTtl));
            write(existing, newTtl);
            log.debug("WSession status aktualisiert id={} status={} ttl={}s", id, newStatus, newTtl.toSeconds());
            return existing;
        });
    }

    public Optional<WSession> updatePlayerUrl(String id, String playerUrl) {
        return get(id).map(existing -> {
            existing.setPlayerUrl(playerUrl);
            existing.touchUpdate();
            Duration ttl = switch (existing.getStatus()) {
                case WAITING -> Duration.ofMinutes(props.getWaitingMinutes());
                case RUNNING -> Duration.ofHours(props.getRunningHours());
                case CLOSED -> Duration.ofMinutes(props.getDeprecatedMinutes());
            };
            existing.setExpireAt(Instant.now().plus(ttl));
            write(existing, ttl);
            log.debug("WSession playerUrl aktualisiert id={} playerUrl={}", id, playerUrl);
            return existing;
        });
    }

    public boolean delete(String id) {
        return Boolean.TRUE.equals(redis.delete(key(id)));
    }

    private void write(WSession session, Duration ttl) {
        var ops = redis.opsForHash();
        var k = key(session.getId());
        ops.put(k, FIELD_STATUS, session.getStatus().name());
        ops.put(k, FIELD_WORLD, session.getWorldId());
        ops.put(k, FIELD_USER, session.getPlayerId());
        if (session.getPlayerUrl() != null) {
            ops.put(k, FIELD_PLAYER_URL, session.getPlayerUrl());
        }
        ops.put(k, FIELD_CREATED, session.getCreatedAt().toString());
        ops.put(k, FIELD_UPDATED, session.getUpdatedAt().toString());
        ops.put(k, FIELD_EXPIRE, session.getExpireAt().toString());
        if (ttl == null || ttl.isNegative() || ttl.isZero()) ttl = Duration.ofSeconds(1);
        redis.expire(k, ttl);
    }

    private String key(String id) { return KEY_PREFIX + id; }

    private String randomId() {
        StringBuilder sb = new StringBuilder(ID_LENGTH);
        for (int i=0; i<ID_LENGTH; i++) {
            sb.append(ID_ALPHABET.charAt(RANDOM.nextInt(ID_ALPHABET.length())));
        }
        return sb.toString();
    }

    /**
     * Räumt abgelaufene Sessions auf. Verwendet Redis SCAN um Speicher zu schonen.
     * Gibt Anzahl gelöschter Keys zurück. Stoppt wenn cleanupMaxDeletes erreicht.
     * @param cursorStart optionaler Cursor ("0" für neuen Durchgang)
     * @return neuer Cursor ("0" wenn Ende erreicht) und gelöschte Anzahl
     */
    public CleanupResult cleanupExpired(String cursorStart) {
        if (!props.isCleanupEnabled()) return new CleanupResult("0",0);
        int deleted = 0;
        var scanOptions = org.springframework.data.redis.core.ScanOptions.scanOptions()
            .match("wsession:*")
            .count(props.getCleanupScanCount())
            .build();
        boolean usedFallback = false;
        try (var connection = redis.getConnectionFactory().getConnection()) {
            Cursor<byte[]> cursor = connection.scan(scanOptions);
            if (cursor != null) {
                while (cursor.hasNext() && deleted < props.getCleanupMaxDeletes()) {
                    String key = new String(cursor.next());
                    deleted += tryDeleteIfExpired(key);
                }
            } else {
                usedFallback = true;
            }
        } catch (Exception e) {
            log.warn("Cleanup Fehler: {}", e.getMessage());
            usedFallback = true; // auf Fallback wechseln
        }
        if (usedFallback) {
            var keys = redis.keys("wsession:*");
            if (keys != null) {
                for (String key : keys) {
                    if (deleted >= props.getCleanupMaxDeletes()) break;
                    deleted += tryDeleteIfExpired(key);
                }
            }
        }
        return new CleanupResult("0", deleted);
    }

    private int tryDeleteIfExpired(String key) {
        Object expireStr = redis.opsForHash().get(key, FIELD_EXPIRE);
        if (expireStr == null) return 0;
        try {
            Instant expireAt = Instant.parse(expireStr.toString());
            if (expireAt.isBefore(Instant.now())) {
                return redis.delete(key) ? 1 : 0;
            }
            return 0;
        } catch (Exception e) {
            return redis.delete(key) ? 1 : 0;
        }
    }

    /**
     * Geplante Bereinigung abgelaufener Sessions. Respektiert cleanupEnabled.
     */
    @Scheduled(fixedDelayString = "#{${world.session.cleanup-interval-seconds:60} * 1000}")
    public void scheduledCleanup() {
        if (!props.isCleanupEnabled()) return;
        try {
            var result = cleanupExpired("0");
            if (result.deleted() > 0) {
                log.debug("Scheduled cleanup removed {} expired sessions", result.deleted());
            }
        } catch (Exception e) {
            log.warn("Scheduled cleanup failed: {}", e.getMessage());
        }
    }

    public record CleanupResult(String cursor, int deleted) { }
}
