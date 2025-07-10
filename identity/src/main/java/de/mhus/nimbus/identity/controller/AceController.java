package de.mhus.nimbus.identity.controller;

import de.mhus.nimbus.identity.entity.Ace;
import de.mhus.nimbus.identity.service.AceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller für Access Control Entities (ACE)
 */
@RestController
@RequestMapping("/api/ace")
@CrossOrigin(origins = "*")
public class AceController {

    @Autowired
    private AceService aceService;

    /**
     * Erstellt eine neue ACE
     */
    @PostMapping
    public ResponseEntity<Ace> createAce(@RequestBody AceCreateRequest request) {
        try {
            Ace ace = aceService.createAce(request.getRule(), request.getUserId(), request.getDescription());
            return new ResponseEntity<>(ace, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Erstellt eine neue ACE mit spezifischer Reihenfolge
     */
    @PostMapping("/with-order")
    public ResponseEntity<Ace> createAceWithOrder(@RequestBody AceCreateWithOrderRequest request) {
        try {
            Ace ace = aceService.createAceWithOrder(
                request.getRule(),
                request.getUserId(),
                request.getOrderValue(),
                request.getDescription()
            );
            return new ResponseEntity<>(ace, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Aktualisiert eine bestehende ACE
     */
    @PutMapping("/{aceId}")
    public ResponseEntity<Ace> updateAce(@PathVariable Long aceId, @RequestBody AceUpdateRequest request) {
        try {
            Ace ace = aceService.updateAce(
                aceId,
                request.getRule(),
                request.getOrderValue(),
                request.getDescription(),
                request.getActive()
            );
            return new ResponseEntity<>(ace, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Löscht eine ACE
     */
    @DeleteMapping("/{aceId}")
    public ResponseEntity<Void> deleteAce(@PathVariable Long aceId) {
        try {
            aceService.deleteAce(aceId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Holt alle ACEs für einen Benutzer
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Ace>> getAcesByUserId(@PathVariable Long userId) {
        try {
            List<Ace> aces = aceService.getAcesByUserId(userId);
            return new ResponseEntity<>(aces, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Holt alle aktiven ACEs für einen Benutzer
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Ace>> getActiveAcesByUserId(@PathVariable Long userId) {
        try {
            List<Ace> aces = aceService.getActiveAcesByUserId(userId);
            return new ResponseEntity<>(aces, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Holt eine ACE anhand der ID
     */
    @GetMapping("/{aceId}")
    public ResponseEntity<Ace> getAceById(@PathVariable Long aceId) {
        Optional<Ace> ace = aceService.getAceById(aceId);
        return ace.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                  .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Sucht ACEs anhand einer Regel
     */
    @GetMapping("/search")
    public ResponseEntity<List<Ace>> searchAcesByRule(@RequestParam String rule) {
        try {
            List<Ace> aces = aceService.searchAcesByRule(rule);
            return new ResponseEntity<>(aces, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Verschiebt eine ACE zu einer neuen Position
     */
    @PatchMapping("/{aceId}/move")
    public ResponseEntity<Ace> moveAce(@PathVariable Long aceId, @RequestBody AceMoveRequest request) {
        try {
            Ace ace = aceService.moveAce(aceId, request.getNewOrderValue());
            return new ResponseEntity<>(ace, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Löscht alle ACEs für einen Benutzer
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllAcesForUser(@PathVariable Long userId) {
        try {
            aceService.deleteAllAcesForUser(userId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Zählt die ACEs für einen Benutzer
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> countAcesByUserId(@PathVariable Long userId) {
        try {
            long count = aceService.countAcesByUserId(userId);
            return new ResponseEntity<>(count, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Request DTOs
    public static class AceCreateRequest {
        private String rule;
        private Long userId;
        private String description;

        public String getRule() { return rule; }
        public void setRule(String rule) { this.rule = rule; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class AceCreateWithOrderRequest extends AceCreateRequest {
        private Integer orderValue;

        public Integer getOrderValue() { return orderValue; }
        public void setOrderValue(Integer orderValue) { this.orderValue = orderValue; }
    }

    public static class AceUpdateRequest {
        private String rule;
        private Integer orderValue;
        private String description;
        private Boolean active;

        public String getRule() { return rule; }
        public void setRule(String rule) { this.rule = rule; }
        public Integer getOrderValue() { return orderValue; }
        public void setOrderValue(Integer orderValue) { this.orderValue = orderValue; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
    }

    public static class AceMoveRequest {
        private Integer newOrderValue;

        public Integer getNewOrderValue() { return newOrderValue; }
        public void setNewOrderValue(Integer newOrderValue) { this.newOrderValue = newOrderValue; }
    }
}
