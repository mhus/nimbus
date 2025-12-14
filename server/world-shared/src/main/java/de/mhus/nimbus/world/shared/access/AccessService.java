package de.mhus.nimbus.world.shared.access;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.shared.types.UserId;
import de.mhus.nimbus.shared.user.WorldRoles;
import de.mhus.nimbus.world.shared.dto.*;
import de.mhus.nimbus.world.shared.region.RCharacter;
import de.mhus.nimbus.world.shared.region.RCharacterService;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for world access management and development authentication.
 *
 * Business Logic Layer:
 * - Provides world and character information
 * - Manages role-based access control
 * - Creates development login tokens (session and agent)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccessService {

    private final WWorldService worldService;
    private final RCharacterService characterService;
    private final de.mhus.nimbus.world.shared.sector.RUserService userService;
    private final JwtService jwtService;
    private final AccessProperties properties;

    // ===== 1. getWorlds =====

    /**
     * Retrieves worlds from the system with optional search filter.
     * Always limits results to prevent overwhelming responses.
     *
     * @param searchQuery Optional search term to filter world names (can be null/empty)
     * @param limit Maximum number of results to return (default 100)
     * @return List of WorldInfoDto with basic world information
     */
    @Transactional(readOnly = true)
    public List<WorldInfoDto> getWorlds(String searchQuery, int limit) {
        log.debug("Fetching worlds with search='{}', limit={}", searchQuery, limit);

        List<WWorld> worlds = worldService.findAll();

        // Filter by search query if provided (searches in worldId, name, and description)
        if (searchQuery != null && !searchQuery.isBlank()) {
            String queryLower = searchQuery.toLowerCase();
            worlds = worlds.stream()
                    .filter(w -> (w.getWorldId() != null && w.getWorldId().toLowerCase().contains(queryLower)) ||
                                 (w.getName() != null && w.getName().toLowerCase().contains(queryLower)) ||
                                 (w.getDescription() != null && w.getDescription().toLowerCase().contains(queryLower)))
                    .collect(Collectors.toList());
        }

        // Always limit results (even without search)
        worlds = worlds.stream()
                .limit(limit)
                .collect(Collectors.toList());

        return worlds.stream()
                .map(this::mapToWorldInfoDto)
                .collect(Collectors.toList());
    }

    /**
     * Maps WWorld entity to WorldInfoDto.
     */
    private WorldInfoDto mapToWorldInfoDto(WWorld world) {
        return WorldInfoDto.builder()
                .worldId(world.getWorldId())
                .name(world.getName())
                .description(world.getDescription())
                .regionId(world.getRegionId())
                .enabled(world.isEnabled())
                .publicFlag(world.isPublicFlag())
                .build();
    }

    // ===== 2. getUsers =====

    /**
     * Retrieves users from the system with optional search filter.
     * Always limits results to prevent overwhelming responses.
     *
     * @param searchQuery Optional search term to filter usernames (can be null/empty)
     * @param limit Maximum number of results to return (default 100)
     * @return List of UserInfoDto with basic user information
     */
    @Transactional(readOnly = true)
    public List<UserInfoDto> getUsers(String searchQuery, int limit) {
        log.debug("Fetching users with search='{}', limit={}", searchQuery, limit);

        List<de.mhus.nimbus.world.shared.sector.RUser> users = userService.listAll();

        // Filter by search query if provided (searches in username and email)
        if (searchQuery != null && !searchQuery.isBlank()) {
            String queryLower = searchQuery.toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(queryLower)) ||
                                 (u.getEmail() != null && u.getEmail().toLowerCase().contains(queryLower)))
                    .collect(Collectors.toList());
        }

        // Always limit results (even without search)
        users = users.stream()
                .limit(limit)
                .collect(Collectors.toList());

        return users.stream()
                .map(this::mapToUserInfoDto)
                .collect(Collectors.toList());
    }

    /**
     * Maps RUser entity to UserInfoDto.
     */
    private UserInfoDto mapToUserInfoDto(de.mhus.nimbus.world.shared.sector.RUser user) {
        return UserInfoDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .build();
    }

    // ===== 3. getCharactersForUserInWorld =====

    /**
     * Retrieves all characters for a user in a specific world.
     *
     * @param userId User ID to search for
     * @param worldId World ID to search in
     * @return List of CharacterInfoDto
     * @throws IllegalArgumentException if world not found
     */
    @Transactional(readOnly = true)
    public List<CharacterInfoDto> getCharactersForUserInWorld(String userId, String worldId) {
        log.debug("Fetching characters for user={} in world={}", userId, worldId);

        // Validate world exists and get regionId
        WWorld world = worldService.getByWorldId(worldId)
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + worldId));

        String regionId = world.getRegionId();
        if (regionId == null || regionId.isBlank()) {
            throw new IllegalStateException("World has no regionId: " + worldId);
        }

        // Fetch characters for user in this region
        List<RCharacter> characters = characterService.listCharacters(userId, regionId);

        return characters.stream()
                .map(this::mapToCharacterInfoDto)
                .collect(Collectors.toList());
    }

    /**
     * Maps RCharacter entity to CharacterInfoDto.
     */
    private CharacterInfoDto mapToCharacterInfoDto(RCharacter character) {
        return CharacterInfoDto.builder()
                .id(character.getId())
                .name(character.getName())
                .display(character.getDisplay())
                .userId(character.getUserId())
                .regionId(character.getRegionId())
                .build();
    }

    // ===== 3. getRolesForCharacterInWorld =====

    /**
     * Retrieves roles for a character in a specific world.
     *
     * @param characterId Character ID (RCharacter.id)
     * @param worldId World ID
     * @return WorldRoleDto with list of WorldRoles
     * @throws IllegalArgumentException if world or character not found
     */
    @Transactional(readOnly = true)
    public WorldRoleDto getRolesForCharacterInWorld(String characterId, String worldId) {
        log.debug("Fetching roles for character={} in world={}", characterId, worldId);

        // Get world
        WWorld world = worldService.getByWorldId(worldId)
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + worldId));

        // Get character to extract userId
        RCharacter character = characterService.listCharactersByRegion(world.getRegionId())
                .stream()
                .filter(c -> c.getId().equals(characterId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + characterId));

        // Get roles from world using userId
        UserId userId = UserId.of(character.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId: " + character.getUserId()));

        List<WorldRoles> roles = world.getRolesForUser(userId);

        return WorldRoleDto.builder()
                .characterId(characterId)
                .worldId(worldId)
                .roles(roles)
                .build();
    }

    // ===== 4. devSessionLogin =====

    /**
     * Creates a session-based access token for development purposes.
     *
     * @param request DevSessionLoginRequest with worldId, userId, characterId, actor
     * @return DevLoginResponse with token, URLs, sessionId, and playerId
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional(readOnly = true)
    public DevLoginResponse devSessionLogin(DevSessionLoginRequest request) {
        log.info("Dev session login: user={}, world={}, character={}, actor={}",
                request.getUserId(), request.getWorldId(), request.getCharacterId(), request.getActor());

        // Validate world exists
        WWorld world = worldService.getByWorldId(request.getWorldId())
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + request.getWorldId()));

        // Validate character exists
        RCharacter character = characterService.listCharactersByRegion(world.getRegionId())
                .stream()
                .filter(c -> c.getId().equals(request.getCharacterId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Character not found: " + request.getCharacterId()));

        // Validate userId matches
        if (!character.getUserId().equals(request.getUserId())) {
            throw new IllegalArgumentException("Character does not belong to user: " + request.getUserId());
        }

        // Generate sessionId and playerId
        String sessionId = UUID.randomUUID().toString();
        String playerId = "@" + request.getUserId() + ":" + request.getCharacterId();

        // Create JWT token
        String token = createSessionToken(
                world.getRegionId(),
                request.getUserId(),
                request.getWorldId(),
                request.getCharacterId(),
                request.getActor().name(),
                sessionId
        );

        // Build response with configured URLs
        return DevLoginResponse.builder()
                .accessToken(token)
                .cookieUrls(properties.getCookieUrls())
                .jumpUrl(findJumpUrl(properties,request, sessionId))
                .sessionId(sessionId)
                .playerId(playerId)
                .build();
    }

    private String findJumpUrl(AccessProperties properties, DevSessionLoginRequest request, String sessionId) {
        var url = properties.getJumpUrlSessionToken();
        url = url.replace("{worldId}", request.getWorldId());
        url = url.replace("{session}", sessionId);
        url = url.replace("{userId}", request.getUserId());
        url = url.replace("{characterId}", request.getCharacterId());
        return null;
    }

    private String findJumpUrl(AccessProperties properties, DevAgentLoginRequest request) {
        var url = properties.getJumpUrlSessionToken();
        url = url.replace("{worldId}", request.getWorldId());
        url = url.replace("{userId}", request.getUserId());
        return null;
    }

    /**
     * Creates a JWT token for session-based access.
     */
    private String createSessionToken(String regionId, String userId, String worldId,
                                      String characterId, String role, String sessionId) {
        // Build claims map
        Map<String, Object> claims = new HashMap<>();
        claims.put("agent", false);
        claims.put("worldId", worldId);
        claims.put("userId", userId);
        claims.put("characterId", characterId);
        claims.put("role", role);
        claims.put("sessionId", sessionId);

        // Calculate expiration
        Instant expiresAt = Instant.now().plusSeconds(properties.getTokenExpirationSeconds());

        // Create token using JwtService
        return jwtService.createTokenWithPrivateKey(
                KeyType.REGION,
                KeyIntent.of(regionId, KeyIntent.REGION_JWT_TOKEN),
                userId,
                claims,
                expiresAt
        );
    }

    // ===== 5. devAgentLogin =====

    /**
     * Creates an agent access token for development purposes.
     * Agent tokens are not bound to a session or character.
     *
     * @param request DevAgentLoginRequest with worldId and userId
     * @return DevLoginResponse with token and URLs (no sessionId/playerId)
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional(readOnly = true)
    public DevLoginResponse devAgentLogin(DevAgentLoginRequest request) {
        log.info("Dev agent login: user={}, world={}", request.getUserId(), request.getWorldId());

        // Validate world exists
        WWorld world = worldService.getByWorldId(request.getWorldId())
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + request.getWorldId()));

        // Create JWT token
        String token = createAgentToken(
                world.getRegionId(),
                request.getUserId(),
                request.getWorldId()
        );

        // Build response with configured URLs (no sessionId/playerId)
        return DevLoginResponse.builder()
                .accessToken(token)
                .cookieUrls(properties.getCookieUrls())
                .jumpUrl(findJumpUrl(properties,request))
                .sessionId(null)
                .playerId(null)
                .build();
    }

    /**
     * Creates a JWT token for agent access.
     */
    private String createAgentToken(String regionId, String userId, String worldId) {
        // Build claims map (agent mode - no characterId, role, or sessionId)
        Map<String, Object> claims = new HashMap<>();
        claims.put("agent", true);
        claims.put("worldId", worldId);
        claims.put("userId", userId);

        // Calculate expiration
        Instant expiresAt = Instant.now().plusSeconds(properties.getTokenExpirationSeconds());

        // Create token using JwtService
        return jwtService.createTokenWithPrivateKey(
                KeyType.REGION,
                KeyIntent.of(regionId, KeyIntent.REGION_JWT_TOKEN),
                userId,
                claims,
                expiresAt
        );
    }
}
