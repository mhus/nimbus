package de.mhus.nimbus.region.world;

import de.mhus.nimbus.region.character.RCharacterService;
import de.mhus.nimbus.shared.dto.region.RegionCharacterResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(RCharacterWorldController.BASE_PATH)
@Validated
@Tag(name = "CharacterWorld", description = "Bereitstellung von Character-Daten für World-Server")
public class RCharacterWorldController {

    public static final String BASE_PATH = "/region/world/character";

    private final RCharacterService characterService;

    public RCharacterWorldController(RCharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping("/{regionId}/{characterId}")
    @Operation(summary = "Character abrufen", description = "Liefert alle Daten eines Characters für den World Server.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gefunden"),
            @ApiResponse(responseCode = "400", description = "Ungültige ID"),
            @ApiResponse(responseCode = "404", description = "Nicht gefunden")
    })
    public ResponseEntity<?> getCharacter(@PathVariable String regionId, @PathVariable String characterId) {
        if (characterId == null || characterId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "characterId blank"));
        }
        if (regionId == null || regionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "regionId blank"));
        }
        var opt = characterService.getByIdAndRegion(characterId, regionId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "character not found"));
        }
        var c = opt.get();
        var resp = new RegionCharacterResponse(
                c.getId(),
                c.getUserId(),
                c.getName(),
                c.getDisplay(),
                c.getBackpack(),
                c.getWearing(),
                c.getSkills(),
                c.getRegionId()
        );
        return ResponseEntity.ok(resp);
    }
}
