package de.mhus.nimbus.region.world;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping(RUniversumWorldController.BASE_PATH)
@Tag(name = "QWorlds", description = "Universum access to worlds and user roles")
public class RUniversumWorldController {

    public static final String BASE_PATH = "/region/universum/world";

    private final RWorldService worldService;

    public RUniversumWorldController(RWorldService worldService) {
        this.worldService = worldService;
    }

    public record WorldAccessResponse(
            String id,
            String worldId,
            String name,
            String description,
            String apiUrl,
            String websocketUrl,
            String controlsUrl,
            String visibility,
            String branch,
            String state,
            List<String> roles
    ) {}

    private WorldAccessResponse toResponse(RWorldService.WorldAccess wa) {
        RWorld w = wa.getWorld();
        return new WorldAccessResponse(
                w.getId(),
                w.getWorldId(),
                w.getName(),
                w.getDescription(),
                w.getApiUrl(),
                w.getWebsocketUrl(),
                w.getControlsUrl(),
                w.getVisibility() == null ? null : w.getVisibility().name(),
                w.getBranch(),
                w.getState() == null ? null : w.getState().name(),
                wa.getRoles().stream().toList()
        );
    }

    @Operation(summary = "List worlds a user can access including their roles in each world")
    @ApiResponse(responseCode = "200", description = "List returned")
    @GetMapping("/access/{userId}")
    public ResponseEntity<List<WorldAccessResponse>> getAccessForUser(@PathVariable String userId) {
        List<RWorldService.WorldAccess> list = worldService.getAccessForUser(userId);
        List<WorldAccessResponse> res = list.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(res);
    }
}
