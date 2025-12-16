package de.mhus.nimbus.world.player.api;

import de.mhus.nimbus.generated.configs.EngineConfiguration;
import de.mhus.nimbus.generated.configs.PlayerBackpack;
import de.mhus.nimbus.generated.configs.Settings;
import de.mhus.nimbus.generated.types.PlayerInfo;
import de.mhus.nimbus.generated.types.WorldInfo;
import de.mhus.nimbus.world.shared.access.AccessValidator;
import de.mhus.nimbus.world.shared.world.WWorld;
import de.mhus.nimbus.world.shared.world.WWorldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * World Configuration REST API
 * Provides complete EngineConfiguration for client initialization.
 */
@RestController
@RequestMapping("/player/world")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "World Config", description = "World configuration for client initialization")
public class WorldConfigController {

    private final WWorldService worldService;
    private final AccessValidator accessUtil;

    @GetMapping("/config")
    @Operation(summary = "Get complete EngineConfiguration",
               description = "Returns worldInfo, playerInfo, playerBackpack, and settings")
    public ResponseEntity<?> getConfig(
            HttpServletRequest request,
            @RequestParam(required = false) String client) {

        var worldId = accessUtil.getWorldId(request).orElseThrow(
                () -> new IllegalStateException("World ID not found in request")
        );

        String clientType = client != null ? client : "viewer";
        log.info("Loading config for world: {}, client: {}", worldId, clientType);

        // Load world from database
        Optional<WWorld> worldOpt = worldService.getByWorldId(worldId);
        if (worldOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "World not found"));
        }

        WWorld world = worldOpt.get();
        WorldInfo worldInfo = world.getPublicData();

        // Create default player info
        PlayerInfo playerInfo = createDefaultPlayerInfo();

        // Create default player backpack
        PlayerBackpack playerBackpack = createDefaultPlayerBackpack();

        // Create default settings
        Settings settings = createDefaultSettings(clientType);

        // Build complete configuration
        EngineConfiguration config = EngineConfiguration.builder()
                .worldInfo(worldInfo)
                .playerInfo(playerInfo)
                .playerBackpack(playerBackpack)
                .settings(settings)
                .build();

        return ResponseEntity.ok(config);
    }

    @GetMapping("/config/worldinfo")
    @Operation(summary = "Get WorldInfo only")
    public ResponseEntity<?> getWorldInfo(HttpServletRequest request) {
        var worldId = accessUtil.getWorldId(request).orElseThrow(
                () -> new IllegalStateException("World ID not found in request")
        );

        Optional<WWorld> worldOpt = worldService.getByWorldId(worldId);
        if (worldOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "World not found"));
        }

        return ResponseEntity.ok(worldOpt.get().getPublicData());
    }

    @GetMapping("/config/playerinfo")
    @Operation(summary = "Get PlayerInfo only")
    public ResponseEntity<?> getPlayerInfo(HttpServletRequest request) {
        var worldId = accessUtil.getWorldId(request).orElseThrow(
                () -> new IllegalStateException("World ID not found in request")
        );

        Optional<WWorld> worldOpt = worldService.getByWorldId(worldId);
        if (worldOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "World not found"));
        }

        return ResponseEntity.ok(createDefaultPlayerInfo());
    }

    @GetMapping("/config/playerbackpack")
    @Operation(summary = "Get PlayerBackpack only")
    public ResponseEntity<?> getPlayerBackpack(HttpServletRequest request) {
        var worldId = accessUtil.getWorldId(request).orElseThrow(
                () -> new IllegalStateException("World ID not found in request")
        );

        Optional<WWorld> worldOpt = worldService.getByWorldId(worldId);
        if (worldOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "World not found"));
        }

        return ResponseEntity.ok(createDefaultPlayerBackpack());
    }

    @GetMapping("/config/settings")
    @Operation(summary = "Get Settings only")
    public ResponseEntity<?> getSettings(
            HttpServletRequest request,
            @RequestParam(required = false) String client) {

        String clientType = client != null ? client : "viewer";

        var worldId = accessUtil.getWorldId(request).orElseThrow(
                () -> new IllegalStateException("World ID not found in request")
        );

        Optional<WWorld> worldOpt = worldService.getByWorldId(worldId);
        if (worldOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "World not found"));
        }

        return ResponseEntity.ok(createDefaultSettings(clientType));
    }

    /**
     * Creates default PlayerInfo with reasonable defaults
     */
    private PlayerInfo createDefaultPlayerInfo() {
        return PlayerInfo.builder()
                .playerId("default-player")
                .displayName("Player")
                .thirdPersonModelId("wizard1")
                .baseWalkSpeed(5.0)
                .baseRunSpeed(7.0)
                .baseUnderwaterSpeed(3.0)
                .baseCrawlSpeed(1.5)
                .baseRidingSpeed(8.0)
                .baseJumpSpeed(8.0)
                .effectiveWalkSpeed(5.0)
                .effectiveRunSpeed(7.0)
                .effectiveUnderwaterSpeed(3.0)
                .effectiveCrawlSpeed(1.5)
                .effectiveRidingSpeed(8.0)
                .effectiveJumpSpeed(8.0)
                .eyeHeight(1.6)
                .stealthRange(8.0)
                .distanceNotifyReductionWalk(0.0)
                .distanceNotifyReductionCrouch(0.5)
                .selectionRadius(5.0)
                .baseTurnSpeed(0.003)
                .effectiveTurnSpeed(0.003)
                .baseUnderwaterTurnSpeed(0.002)
                .effectiveUnderwaterTurnSpeed(0.002)
                .build();
    }

    /**
     * Creates default PlayerBackpack (empty)
     */
    private PlayerBackpack createDefaultPlayerBackpack() {
        return PlayerBackpack.builder()
                .itemIds(new HashMap<>())
                .wearingItemIds(new HashMap<>())
                .build();
    }

    /**
     * Creates default Settings based on client type
     */
    private Settings createDefaultSettings(String clientType) {
        return Settings.builder()
                .name("Player")
                .inputController("keyboard")
                .inputMappings(new HashMap<>())
                .build();
    }
}
