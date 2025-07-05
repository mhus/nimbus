package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.entity.PlayerCharacter;
import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.shared.avro.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service für Kafka-basierte Identity-Lookup-Operationen
 * Trennt die Kafka-Kommunikation von der Business-Logik
 */
@Service
@Transactional(readOnly = true)
public class IdentityLookupService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityLookupService.class);

    private final UserService userService;
    private final PlayerCharacterService playerCharacterService;

    public IdentityLookupService(UserService userService, PlayerCharacterService playerCharacterService) {
        this.userService = userService;
        this.playerCharacterService = playerCharacterService;
    }

    /**
     * Verarbeitet eine User-Lookup-Anfrage
     */
    public UserLookupResponse processUserLookupRequest(UserLookupRequest request) {
        logger.info("Processing user lookup request: requestId={}, userId={}, username={}, email={}, requestedBy={}",
                   request.getRequestId(), request.getUserId(), request.getUsername(),
                   request.getEmail(), request.getRequestedBy());

        long currentTimestamp = Instant.now().toEpochMilli();

        try {
            Optional<User> userOpt = findUser(request);

            if (userOpt.isEmpty()) {
                return UserLookupResponse.newBuilder()
                        .setRequestId(request.getRequestId())
                        .setStatus(UserLookupStatus.USER_NOT_FOUND)
                        .setUser(null)
                        .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                        .setErrorMessage("User not found")
                        .build();
            }

            User user = userOpt.get();
            UserInfo userInfo = createUserInfo(user);

            return UserLookupResponse.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setStatus(UserLookupStatus.SUCCESS)
                    .setUser(userInfo)
                    .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                    .setErrorMessage(null)
                    .build();

        } catch (Exception e) {
            logger.error("Error processing user lookup request: {}", request.getRequestId(), e);
            return createUserLookupErrorResponse(request, e.getMessage(), currentTimestamp);
        }
    }

    /**
     * Verarbeitet eine PlayerCharacter-Lookup-Anfrage
     */
    public PlayerCharacterLookupResponse processPlayerCharacterLookupRequest(PlayerCharacterLookupRequest request) {
        logger.info("Processing player character lookup request: requestId={}, characterId={}, characterName={}, userId={}, requestedBy={}",
                   request.getRequestId(), request.getCharacterId(), request.getCharacterName(),
                   request.getUserId(), request.getRequestedBy());

        long currentTimestamp = Instant.now().toEpochMilli();

        try {
            List<PlayerCharacter> characters = findPlayerCharacters(request);
            List<PlayerCharacterInfo> characterInfos = characters.stream()
                    .map(this::createPlayerCharacterInfo)
                    .toList();

            PlayerCharacterLookupStatus status = characters.isEmpty() ?
                    PlayerCharacterLookupStatus.CHARACTER_NOT_FOUND :
                    PlayerCharacterLookupStatus.SUCCESS;

            return PlayerCharacterLookupResponse.newBuilder()
                    .setRequestId(request.getRequestId())
                    .setStatus(status)
                    .setCharacters(characterInfos)
                    .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
                    .setErrorMessage(status == PlayerCharacterLookupStatus.CHARACTER_NOT_FOUND ?
                            "No characters found matching criteria" : null)
                    .build();

        } catch (Exception e) {
            logger.error("Error processing player character lookup request: {}", request.getRequestId(), e);
            return createPlayerCharacterLookupErrorResponse(request, e.getMessage(), currentTimestamp);
        }
    }

    /**
     * Findet einen User basierend auf der Anfrage
     */
    private Optional<User> findUser(UserLookupRequest request) {
        if (request.getUserId() != null) {
            return userService.findById(request.getUserId());
        } else if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            return userService.findByUsername(request.getUsername().trim());
        } else if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            return userService.findByEmail(request.getEmail().trim());
        }
        return Optional.empty();
    }

    /**
     * Findet PlayerCharacters basierend auf der Anfrage
     */
    private List<PlayerCharacter> findPlayerCharacters(PlayerCharacterLookupRequest request) {
        if (request.getCharacterId() != null) {
            return playerCharacterService.findById(request.getCharacterId())
                    .filter(character -> !request.getActiveOnly() || character.getActive())
                    .map(List::of)
                    .orElse(List.of());
        } else if (request.getCharacterName() != null && !request.getCharacterName().trim().isEmpty()) {
            return playerCharacterService.findByName(request.getCharacterName().trim())
                    .filter(character -> !request.getActiveOnly() || character.getActive())
                    .map(List::of)
                    .orElse(List.of());
        } else if (request.getUserId() != null) {
            Optional<User> userOpt = userService.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                return List.of();
            }
            return request.getActiveOnly() ?
                    playerCharacterService.findActiveCharactersByUser(userOpt.get()) :
                    playerCharacterService.findByUser(userOpt.get());
        } else if (request.getCurrentPlanet() != null && !request.getCurrentPlanet().trim().isEmpty()) {
            List<PlayerCharacter> characters = playerCharacterService.findByPlanet(request.getCurrentPlanet().trim());
            return request.getActiveOnly() ?
                    characters.stream().filter(PlayerCharacter::getActive).toList() :
                    characters;
        } else if (request.getCurrentWorldId() != null && !request.getCurrentWorldId().trim().isEmpty()) {
            List<PlayerCharacter> characters = playerCharacterService.findByWorld(request.getCurrentWorldId().trim());
            return request.getActiveOnly() ?
                    characters.stream().filter(PlayerCharacter::getActive).toList() :
                    characters;
        }
        return List.of();
    }

    /**
     * Erstellt UserInfo aus User Entity
     */
    private UserInfo createUserInfo(User user) {
        return UserInfo.newBuilder()
                .setId(user.getId())
                .setUsername(user.getUsername())
                .setEmail(user.getEmail())
                .setFirstName(user.getFirstName())
                .setLastName(user.getLastName())
                .setActive(user.getActive())
                .setCreatedAt(user.getCreatedAt())
                .setUpdatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Erstellt PlayerCharacterInfo aus PlayerCharacter Entity
     */
    private PlayerCharacterInfo createPlayerCharacterInfo(PlayerCharacter character) {
        return PlayerCharacterInfo.newBuilder()
                .setId(character.getId())
                .setName(character.getName())
                .setDescription(character.getDescription())
                .setCharacterClass(character.getCharacterClass())
                .setLevel(character.getLevel())
                .setExperiencePoints(character.getExperiencePoints())
                .setHealthPoints(character.getHealthPoints())
                .setMaxHealthPoints(character.getMaxHealthPoints())
                .setManaPoints(character.getManaPoints())
                .setMaxManaPoints(character.getMaxManaPoints())
                .setCurrentWorldId(character.getCurrentWorldId())
                .setCurrentPlanet(character.getCurrentPlanet())
                .setPositionX(character.getPositionX())
                .setPositionY(character.getPositionY())
                .setPositionZ(character.getPositionZ())
                .setActive(character.getActive())
                .setLastLogin(character.getLastLogin() != null ?  character.getLastLogin() : null)
                .setCreatedAt(character.getCreatedAt())
                .setUpdatedAt(character.getUpdatedAt())
                .setUserId(character.getUser().getId())
                .build();
    }

    /**
     * Validiert eine User-Lookup-Anfrage
     */
    public void validateUserLookupRequest(UserLookupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("User lookup request cannot be null");
        }

        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        // Mindestens eines der Suchkriterien muss angegeben sein
        if (request.getUserId() == null &&
            (request.getUsername() == null || request.getUsername().trim().isEmpty()) &&
            (request.getEmail() == null || request.getEmail().trim().isEmpty())) {
            throw new IllegalArgumentException("At least one search criteria (userId, username, or email) must be provided");
        }

        logger.debug("User lookup request validation passed: {}", request.getRequestId());
    }

    /**
     * Validiert eine PlayerCharacter-Lookup-Anfrage
     */
    public void validatePlayerCharacterLookupRequest(PlayerCharacterLookupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Player character lookup request cannot be null");
        }

        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be null or empty");
        }

        // Mindestens eines der Suchkriterien muss angegeben sein
        if (request.getCharacterId() == null &&
            (request.getCharacterName() == null || request.getCharacterName().trim().isEmpty()) &&
            request.getUserId() == null &&
            (request.getCurrentPlanet() == null || request.getCurrentPlanet().trim().isEmpty()) &&
            (request.getCurrentWorldId() == null || request.getCurrentWorldId().trim().isEmpty())) {
            throw new IllegalArgumentException("At least one search criteria must be provided");
        }

        logger.debug("Player character lookup request validation passed: {}", request.getRequestId());
    }

    /**
     * Erstellt eine Error-Response für User-Lookup-Fehler
     */
    public UserLookupResponse createUserLookupErrorResponse(UserLookupRequest request, String errorMessage) {
        long currentTimestamp = Instant.now().toEpochMilli();
        return createUserLookupErrorResponse(request, errorMessage, currentTimestamp);
    }

    private UserLookupResponse createUserLookupErrorResponse(UserLookupRequest request, String errorMessage, long timestamp) {
        return UserLookupResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setStatus(UserLookupStatus.ERROR)
                .setUser(null)
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .setErrorMessage(errorMessage)
                .build();
    }

    /**
     * Erstellt eine Error-Response für PlayerCharacter-Lookup-Fehler
     */
    public PlayerCharacterLookupResponse createPlayerCharacterLookupErrorResponse(PlayerCharacterLookupRequest request, String errorMessage) {
        long currentTimestamp = Instant.now().toEpochMilli();
        return createPlayerCharacterLookupErrorResponse(request, errorMessage, currentTimestamp);
    }

    private PlayerCharacterLookupResponse createPlayerCharacterLookupErrorResponse(PlayerCharacterLookupRequest request, String errorMessage, long timestamp) {
        return PlayerCharacterLookupResponse.newBuilder()
                .setRequestId(request.getRequestId())
                .setStatus(PlayerCharacterLookupStatus.ERROR)
                .setCharacters(new ArrayList<>())
                .setTimestamp(Instant.ofEpochMilli(timestamp))
                .setErrorMessage(errorMessage)
                .build();
    }
}
