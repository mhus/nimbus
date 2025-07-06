package de.mhus.nimbus.identity.controller;

import de.mhus.nimbus.identity.entity.IdentityCharacter;
import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.service.IdentityCharacterService;
import de.mhus.nimbus.identity.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller für IdentityCharacter-Management
 */
@RestController
@RequestMapping("/api/characters")
public class IdentityCharacterController {

    private final IdentityCharacterService identityCharacterService;
    private final UserService userService;

    public IdentityCharacterController(IdentityCharacterService identityCharacterService, UserService userService) {
        this.identityCharacterService = identityCharacterService;
        this.userService = userService;
    }

    /**
     * Erstellt einen neuen IdentityCharacter
     */
    @PostMapping
    public ResponseEntity<IdentityCharacter> createCharacter(@RequestBody CreateCharacterRequest request) {
        try {
            User user = userService.findById(request.userId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            IdentityCharacter character = identityCharacterService.createIdentityCharacter(
                    user, request.name(), request.characterClass(), request.description());

            return ResponseEntity.status(HttpStatus.CREATED).body(character);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Findet einen IdentityCharacter anhand der ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<IdentityCharacter> getCharacterById(@PathVariable Long id) {
        Optional<IdentityCharacter> character = identityCharacterService.findById(id);
        return character.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Findet einen IdentityCharacter anhand des Namens
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<IdentityCharacter> getCharacterByName(@PathVariable String name) {
        Optional<IdentityCharacter> character = identityCharacterService.findByName(name);
        return character.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Findet alle IdentityCharacters eines Users
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<IdentityCharacter>> getCharactersByUser(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            List<IdentityCharacter> characters = identityCharacterService.findByUser(user);
            return ResponseEntity.ok(characters);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Findet alle aktiven IdentityCharacters eines Users
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<IdentityCharacter>> getActiveCharactersByUser(@PathVariable Long userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            List<IdentityCharacter> characters = identityCharacterService.findActiveCharactersByUser(user);
            return ResponseEntity.ok(characters);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Aktualisiert die Position eines IdentityCharacters
     */
    @PutMapping("/{id}/position")
    public ResponseEntity<IdentityCharacter> updatePosition(@PathVariable Long id,
                                                           @RequestBody UpdatePositionRequest request) {
        try {
            IdentityCharacter character = identityCharacterService.updatePosition(
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
    public ResponseEntity<IdentityCharacter> updateLevel(@PathVariable Long id,
                                                        @RequestBody UpdateLevelRequest request) {
        try {
            IdentityCharacter character = identityCharacterService.updateLevelAndExperience(
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
    public ResponseEntity<IdentityCharacter> updateStats(@PathVariable Long id,
                                                        @RequestBody UpdateStatsRequest request) {
        try {
            IdentityCharacter character = identityCharacterService.updateHealthAndMana(
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
    public ResponseEntity<IdentityCharacter> updateLastLogin(@PathVariable Long id) {
        try {
            IdentityCharacter character = identityCharacterService.updateLastLogin(id);
            return ResponseEntity.ok(character);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deaktiviert einen IdentityCharacter
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCharacter(@PathVariable Long id) {
        try {
            identityCharacterService.deactivateIdentityCharacter(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Reaktiviert einen IdentityCharacter
     */
    @PutMapping("/{id}/reactivate")
    public ResponseEntity<IdentityCharacter> reactivateCharacter(@PathVariable Long id) {
        try {
            IdentityCharacter character = identityCharacterService.reactivateIdentityCharacter(id);
            return ResponseEntity.ok(character);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Request DTOs
    public record CreateCharacterRequest(Long userId, String name, String characterClass, String description) {}
    public record UpdatePositionRequest(String worldId, String planet, Double x, Double y, Double z) {}
    public record UpdateLevelRequest(Integer level, Long experiencePoints) {}
    public record UpdateStatsRequest(Integer healthPoints, Integer manaPoints) {}
}
