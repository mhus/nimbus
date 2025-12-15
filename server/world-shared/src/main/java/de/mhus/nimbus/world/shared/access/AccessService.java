package de.mhus.nimbus.world.shared.access;

import de.mhus.nimbus.shared.security.Base64Service;
import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.shared.types.UserId;
import de.mhus.nimbus.shared.types.WorldId;
import de.mhus.nimbus.shared.user.WorldRoles;
import de.mhus.nimbus.world.shared.dto.*;
import de.mhus.nimbus.world.shared.region.RCharacter;
import de.mhus.nimbus.world.shared.region.RCharacterService;
import de.mhus.nimbus.world.shared.sector.RUserService;
import de.mhus.nimbus.world.shared.session.WSession;
import de.mhus.nimbus.world.shared.session.WSessionStatus;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final de.mhus.nimbus.world.shared.session.WSessionService sessionService;
    private final JwtService jwtService;
    private final AccessProperties properties;
    private final Base64Service base64Service;

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
        claims.put("regionId", regionId);

        // Calculate expiration (use sessionTokenTtlSeconds for session tokens)
        Instant expiresAt = Instant.now().plusSeconds(properties.getSessionTokenTtlSeconds());

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
        claims.put("regionId", regionId);

        // Calculate expiration (use agentTokenTtlSeconds for agent tokens)
        Instant expiresAt = Instant.now().plusSeconds(properties.getAgentTokenTtlSeconds());

        // Create token using JwtService
        return jwtService.createTokenWithPrivateKey(
                KeyType.REGION,
                KeyIntent.of(regionId, KeyIntent.REGION_JWT_TOKEN),
                userId,
                claims,
                expiresAt
        );
    }

    // ===== 6. authorizeWithToken =====

    /**
     * Internal record for parsed access token claims.
     */
    private record AccessTokenClaims(
            boolean agent,
            String worldId,
            String userId,
            String characterId,  // null for agent
            String role,         // null for agent
            String sessionId,    // null for agent
            String regionId
    ) {}

    /**
     * Validates access token and creates session cookies.
     *
     * @param accessToken The access token from /devlogin
     * @param response HttpServletResponse for cookie manipulation
     * @throws IllegalArgumentException if token invalid or world not found
     * @throws IllegalStateException if session invalid or access denied
     */
    @Transactional
    public void authorizeWithToken(String accessToken, HttpServletResponse response) {
        log.debug("Authorizing with access token");

        // 1. Validate and parse access token
        AccessTokenClaims claims = validateAndParseAccessToken(accessToken);

        // 2. Get world
        WWorld world = worldService.getByWorldId(claims.worldId())
                .orElseThrow(() -> new IllegalArgumentException("World not found: " + claims.worldId()));

        // 3. Validate based on agent flag
        if (claims.agent()) {
            validateAgentAccess(claims.userId(), world);
        } else {
            validateSessionAccess(claims, world);
        }

        // 4. Create session token
        SessionTokenWithExpiry tokenWithExpiry = createSessionTokenFromAccess(claims, claims.regionId());

        // 5. Set cookies
        setCookies(response, tokenWithExpiry.token(), tokenWithExpiry.expiresAt(), claims);

        log.info("Authorization successful - worldId={}, userId={}, agent={}",
                claims.worldId(), claims.userId(), claims.agent());
    }

    /**
     * Validates access token and extracts claims.
     */
    private AccessTokenClaims validateAndParseAccessToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }

        // Parse token to extract regionId from claims (unverified)
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid token format");
        }

        // Decode payload to extract regionId
        String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        String regionId = extractRegionIdFromJson(payloadJson);

        // Now validate token with correct regionId
        Optional<Jws<Claims>> jwsOpt = jwtService.validateTokenWithPublicKey(
                token,
                KeyType.REGION,
                KeyIntent.of(regionId, KeyIntent.REGION_JWT_TOKEN)
        );

        if (jwsOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired access token");
        }

        Claims claims = jwsOpt.get().getPayload();

        // Extract claims
        Boolean agent = claims.get("agent", Boolean.class);
        String worldId = claims.get("worldId", String.class);
        String userId = claims.get("userId", String.class);
        String characterId = claims.get("characterId", String.class);
        String role = claims.get("role", String.class);
        String sessionId = claims.get("sessionId", String.class);

        // Validate required fields
        if (agent == null || worldId == null || userId == null) {
            throw new IllegalArgumentException("Access token missing required claims");
        }

        // Session tokens require additional fields
        if (!agent && (characterId == null || role == null || sessionId == null)) {
            throw new IllegalArgumentException("Session access token missing required claims");
        }

        return new AccessTokenClaims(agent, worldId, userId, characterId, role, sessionId, regionId);
    }

    /**
     * Extracts regionId from JSON payload (simple string matching).
     */
    private String extractRegionIdFromJson(String json) {
        int regionIdIndex = json.indexOf("\"regionId\"");
        if (regionIdIndex == -1) {
            throw new IllegalArgumentException("Token missing regionId claim");
        }

        int valueStart = json.indexOf("\"", regionIdIndex + 11);
        int valueEnd = json.indexOf("\"", valueStart + 1);

        if (valueStart == -1 || valueEnd == -1) {
            throw new IllegalArgumentException("Invalid regionId claim format");
        }

        return json.substring(valueStart + 1, valueEnd);
    }

    /**
     * Validates session access by checking Redis session.
     */
    private void validateSessionAccess(AccessTokenClaims claims, WWorld world) {
        log.debug("Validating session access - sessionId={}", claims.sessionId());

        // Get session from Redis
        WSession session = sessionService.get(claims.sessionId())
                .orElseThrow(() -> new IllegalStateException("Session not found: " + claims.sessionId()));

        // Check status = WAITING
        if (session.getStatus() != WSessionStatus.WAITING) {
            throw new IllegalStateException("Session must be WAITING, found: " + session.getStatus());
        }

        // Check worldId matches
        if (!claims.worldId().equals(session.getWorldId())) {
            throw new IllegalStateException("Session worldId mismatch");
        }

        // Check playerId matches
        String expectedPlayerId = "@" + claims.userId() + ":" + claims.characterId();
        if (!expectedPlayerId.equals(session.getPlayerId())) {
            throw new IllegalStateException("Session playerId mismatch");
        }

        // Update session to RUNNING
        sessionService.updateStatus(claims.sessionId(), WSessionStatus.RUNNING);

        log.debug("Session validated and activated - sessionId={}", claims.sessionId());
    }

    /**
     * Validates agent access by checking user permissions in world.
     */
    private void validateAgentAccess(String userId, WWorld world) {
        log.debug("Validating agent access - userId={}, worldId={}", userId, world.getWorldId());

        UserId userIdObj = UserId.of(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId: " + userId));

        var userOpt = userService.getByUsername(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        var user = userOpt.get();
        var worldIdOpt = WorldId.of(world.getWorldId());
        if (worldIdOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid worldId: " + world.getWorldId());
        }

        boolean hasAccess =
                user.isSectorAdmin() ||
                user.isRegionAdmin(worldIdOpt.get()) ||
                world.isOwnerAllowed(userIdObj) ||
                world.isEditorAllowed(userIdObj) ||
                world.isSupporterAllowed(userIdObj);

        if (!hasAccess) {
            throw new IllegalStateException("User not authorized for agent access to world: " + world.getWorldId());
        }

        log.debug("Agent access validated - userId={}", userId);
    }

    /**
     * Creates session token from validated access token claims.
     */
    private record SessionTokenWithExpiry(String token, Instant expiresAt) {}

    private SessionTokenWithExpiry createSessionTokenFromAccess(AccessTokenClaims claims, String regionId) {
        Map<String, Object> tokenClaims = new HashMap<>();
        tokenClaims.put("agent", claims.agent());
        tokenClaims.put("worldId", claims.worldId());
        tokenClaims.put("userId", claims.userId());
        tokenClaims.put("regionId", regionId);

        if (!claims.agent()) {
            tokenClaims.put("characterId", claims.characterId());
            tokenClaims.put("role", claims.role());
            tokenClaims.put("sessionId", claims.sessionId());
        }

        // TTL: 24h (session) or 1h (agent)
        long ttlSeconds = claims.agent()
                ? properties.getAgentTokenTtlSeconds()
                : properties.getSessionTokenTtlSeconds();

        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);

        String token = jwtService.createTokenWithPrivateKey(
                KeyType.REGION,
                KeyIntent.of(regionId, KeyIntent.REGION_JWT_TOKEN),
                claims.userId(),
                tokenClaims,
                expiresAt
        );

        return new SessionTokenWithExpiry(token, expiresAt);
    }

    /**
     * Sets two cookies: sessionToken (httpOnly) and sessionData (JS-accessible).
     */
    private void setCookies(HttpServletResponse response, String sessionToken, Instant expiresAt, AccessTokenClaims claims) {
        // Calculate maxAge from the difference between expiration and now (in UTC)
        long maxAgeSeconds = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();

        boolean secure = properties.isSecureCookies();
        String domain = properties.getCookieDomain();

        // Build Set-Cookie headers manually to include SameSite attribute
        String tokenCookieHeader = buildCookieHeader("sessionToken", sessionToken, secure, domain, (int) maxAgeSeconds, true);
        response.addHeader("Set-Cookie", tokenCookieHeader);

        // Cookie 2: sessionData (JS-accessible, Base64-encoded)
        String sessionData = buildSessionDataJson(claims);
        String encodedSessionData = base64Service.encode(sessionData);
        String dataCookieHeader = buildCookieHeader("sessionData", encodedSessionData, secure, domain, (int) maxAgeSeconds, false);
        response.addHeader("Set-Cookie", dataCookieHeader);

        log.info("Cookies set - sessionToken (httpOnly, secure={}, domain='{}', path='/', maxAge={}, expiresAt={}), sessionData (JS-accessible, Base64-encoded, same settings)",
                secure, domain != null ? domain : "(none)", maxAgeSeconds, expiresAt);
    }

    /**
     * Builds Set-Cookie header with SameSite attribute.
     */
    private String buildCookieHeader(String name, String value, boolean secure, String domain, int maxAge, boolean httpOnly) {
        StringBuilder cookie = new StringBuilder();
        cookie.append(name).append("=").append(value);
        cookie.append("; Path=/");
        cookie.append("; Max-Age=").append(maxAge);

        if (domain != null && !domain.isBlank()) {
            cookie.append("; Domain=").append(domain);
        }

        if (secure) {
            cookie.append("; Secure");
        }

        if (httpOnly) {
            cookie.append("; HttpOnly");
        }

        // SameSite=None allows cross-site cookies (required for CORS), but needs Secure=true
        // For development without HTTPS, use SameSite=Lax
        if (secure) {
            cookie.append("; SameSite=None");
        } else {
            cookie.append("; SameSite=Lax");
        }

        return cookie.toString();
    }

    /**
     * Builds JSON string for sessionData cookie.
     */
    private String buildSessionDataJson(AccessTokenClaims claims) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"worldId\":\"").append(claims.worldId()).append("\"");
        json.append(",\"userId\":\"").append(claims.userId()).append("\"");
        json.append(",\"agent\":").append(claims.agent());

        if (!claims.agent()) {
            json.append(",\"sessionId\":\"").append(claims.sessionId()).append("\"");
            json.append(",\"characterId\":\"").append(claims.characterId()).append("\"");
            json.append(",\"role\":\"").append(claims.role()).append("\"");
        }

        json.append("}");
        return json.toString();
    }
}
