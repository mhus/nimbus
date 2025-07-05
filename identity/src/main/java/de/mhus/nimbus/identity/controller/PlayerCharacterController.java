package de.mhus.nimbus.identity.controller;

import de.mhus.nimbus.identity.entity.PlayerCharacter;
import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.service.PlayerCharacterService;
import de.mhus.nimbus.identity.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller für PlayerCharacter-Management
 */
@RestController
@RequestMapping("/api/characters")
public class PlayerCharacterController {

    private final PlayerCharacterService playerCharacterService;
    private final UserService userService;

    public PlayerCharacterController(PlayerCharacterService playerCharacterService, UserService userService) {
        this.playerCharacterService = playerCharacterService;
        this.userService = userService;
    }

    /**
     * Erstellt einen neuen PlayerCharacter
     */
    @PostMapping
    public ResponseEntity<PlayerCharacter> createCharacter(@RequestBody CreateCharacterRequest request) {
        try {
            User user = userService.findById(request.userId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            PlayerCharacter character = playerCharacterService.createPlayerCharacter(
                    user, request.name(), request.characterClass(), request.description());

            return ResponseEntity.status(HttpStatus.CREATED).body(character);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Findet einen PlayerCharacter anhand der ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PlayerCharacter> getCharacterById(@PathVariable Long id) {
        Optional<PlayerCharacter> character = playerCharacterService.findById(id);
        return character.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Findet einen PlayerCharacter anhand des Namens
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<PlayerCharacter> getCharacterByName(@PathVariable String name) {
        Optional<PlayerCharacter> character = playerCharacterService.findByName(name);
        return character.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Findet alle PlayerCharacters eines Users
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PlayerCharacter>> getCharactersByUser(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            List<PlayerCharacter> characters = playerCharacterService.findByUser(user);
            return ResponseEntity.ok(characters);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Findet alle aktiven PlayerCharacters eines Users
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<PlayerCharacter>> getActiveCharactersByUser(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            List<PlayerCharacter> characters = playerCharacterService.findActiveCharactersByUser(user);
            return ResponseEntity.ok(characters);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Aktualisiert die Position eines PlayerCharacters
     */
    @PutMapping("/{id}/position")
    public ResponseEntity<PlayerCharacter> updatePosition(@PathVariable Long id,
                                                         @RequestBody UpdatePositionRequest request) {
        try {
            PlayerCharacter character = playerCharacterService.updatePosition(
                    id, request.worldId(), request.planet(),
                    request.x(), request.y(), request.z());
            return ResponseEntity.ok(character);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Aktualisiert Level und Erfahrungspunkte
     */
    @PutMapping("/{id}/level")
    public ResponseEntity<PlayerCharacter> updateLevel(@PathVariable Long id,
                                                      @RequestBody UpdateLevelRequest request) {
        try {
            PlayerCharacter character = playerCharacterService.updateLevelAndExperience(
                    id, request.level(), request.experiencePoints());
            return ResponseEntity.ok(character);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Aktualisiert Gesundheits- und Manapunkte
     */
    @PutMapping("/{id}/stats")
    public ResponseEntity<PlayerCharacter> updateStats(@PathVariable Long id,
                                                      @RequestBody UpdateStatsRequest request) {
        try {
            PlayerCharacter character = playerCharacterService.updateHealthAndMana(
                    id, request.healthPoints(), request.manaPoints());
            return ResponseEntity.ok(character);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Setzt den letzten Login-Zeitstempel
     */
    @PutMapping("/{id}/login")
    public ResponseEntity<PlayerCharacter> updateLastLogin(@PathVariable Long id) {
        try {
            PlayerCharacter character = playerCharacterService.updateLastLogin(id);
            return ResponseEntity.ok(character);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deaktiviert einen PlayerCharacter
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateCharacter(@PathVariable Long id) {
        try {
            playerCharacterService.deactivatePlayerCharacter(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reaktiviert einen PlayerCharacter
     */
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<PlayerCharacter> reactivateCharacter(@PathVariable Long id) {
        try {
            PlayerCharacter character = playerCharacterService.reactivatePlayerCharacter(id);
            return ResponseEntity.ok(character);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Findet PlayerCharacters auf einem bestimmten Planeten
     */
    @GetMapping("/planet/{planet}")
    public ResponseEntity<List<PlayerCharacter>> getCharactersByPlanet(@PathVariable String planet) {
        List<PlayerCharacter> characters = playerCharacterService.findByPlanet(planet);
        return ResponseEntity.ok(characters);
    }

    /**
     * Findet PlayerCharacters in einer bestimmten Welt
     */
    @GetMapping("/world/{worldId}")
    public ResponseEntity<List<PlayerCharacter>> getCharactersByWorld(@PathVariable String worldId) {
        List<PlayerCharacter> characters = playerCharacterService.findByWorld(worldId);
        return ResponseEntity.ok(characters);
    }

    // Request DTOs
    public record CreateCharacterRequest(
            Long userId,
            String name,
            String characterClass,
            String description
    ) {}

    public record UpdatePositionRequest(
            String worldId,
            String planet,
            Double x,
            Double y,
            Double z
    ) {}

    public record UpdateLevelRequest(
            Integer level,
            Long experiencePoints
    ) {}

    public record UpdateStatsRequest(
            Integer healthPoints,
            Integer manaPoints
    ) {}
}
