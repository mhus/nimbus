package de.mhus.nimbus.quadrant.world;

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
@RequestMapping(QUniversumWorldController.BASE_PATH)
@Tag(name = "QWorlds", description = "Universum access to worlds and user roles")
public class QUniversumWorldController {

    public static final String BASE_PATH = "/quadrant/universum/world";

    private final QWorldService worldService;

    public QUniversumWorldController(QWorldService worldService) {
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

    private WorldAccessResponse toResponse(QWorldService.WorldAccess wa) {
        QWorld w = wa.getWorld();
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
        List<QWorldService.WorldAccess> list = worldService.getAccessForUser(userId);
        List<WorldAccessResponse> res = list.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(res);
    }
}
