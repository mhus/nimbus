package de.mhus.nimbus.identity.service;

import de.mhus.nimbus.identity.entity.Ace;
import de.mhus.nimbus.identity.entity.User;
import de.mhus.nimbus.identity.repository.AceRepository;
import de.mhus.nimbus.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service für Access Control Entities (ACE)
 */
@Service
@Transactional
public class AceService {

    @Autowired
    private AceRepository aceRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Erstellt eine neue ACE
     */
    public Ace createAce(String rule, Long userId, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Bestimme die nächste Reihenfolge-Nummer
        Integer nextOrder = getNextOrderValue(userId);

        Ace ace = new Ace(rule, nextOrder, user);
        ace.setDescription(description);

        return aceRepository.save(ace);
    }

    /**
     * Erstellt eine neue ACE mit spezifischer Reihenfolge
     */
    public Ace createAceWithOrder(String rule, Long userId, Integer orderValue, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Prüfe ob die Reihenfolge bereits existiert
        if (aceRepository.existsByUserAndOrderValue(user, orderValue)) {
            // Verschiebe bestehende ACEs nach unten
            shiftOrderValues(userId, orderValue, 1);
        }

        Ace ace = new Ace(rule, orderValue, user);
        ace.setDescription(description);

        return aceRepository.save(ace);
    }

    /**
     * Aktualisiert eine bestehende ACE
     */
    public Ace updateAce(Long aceId, String rule, Integer orderValue, String description, Boolean active) {
        Ace ace = aceRepository.findById(aceId)
                .orElseThrow(() -> new RuntimeException("ACE not found with id: " + aceId));

        // Wenn sich die Reihenfolge ändert, handhabe die Verschiebung
        if (orderValue != null && !orderValue.equals(ace.getOrderValue())) {
            handleOrderChange(ace, orderValue);
        }

        if (rule != null) ace.setRule(rule);
        if (description != null) ace.setDescription(description);
        if (active != null) ace.setActive(active);

        return aceRepository.save(ace);
    }

    /**
     * Löscht eine ACE
     */
    public void deleteAce(Long aceId) {
        Ace ace = aceRepository.findById(aceId)
                .orElseThrow(() -> new RuntimeException("ACE not found with id: " + aceId));

        aceRepository.delete(ace);

        // Schließe Lücken in der Reihenfolge
        compactOrderValues(ace.getUser().getId());
    }

    /**
     * Findet alle ACEs für einen Benutzer
     */
    @Transactional(readOnly = true)
    public List<Ace> getAcesByUserId(Long userId) {
        return aceRepository.findByUserIdOrderByOrderValueAsc(userId);
    }

    /**
     * Findet alle aktiven ACEs für einen Benutzer
     */
    @Transactional(readOnly = true)
    public List<Ace> getActiveAcesByUserId(Long userId) {
        return aceRepository.findByUserIdAndActiveOrderByOrderValueAsc(userId, true);
    }

    /**
     * Findet eine ACE anhand der ID
     */
    @Transactional(readOnly = true)
    public Optional<Ace> getAceById(Long aceId) {
        return aceRepository.findById(aceId);
    }

    /**
     * Sucht ACEs anhand einer Regel
     */
    @Transactional(readOnly = true)
    public List<Ace> searchAcesByRule(String rulePattern) {
        return aceRepository.findByRuleContainingIgnoreCase(rulePattern);
    }

    /**
     * Verschiebt eine ACE zu einer neuen Position
     */
    public Ace moveAce(Long aceId, Integer newOrderValue) {
        Ace ace = aceRepository.findById(aceId)
                .orElseThrow(() -> new RuntimeException("ACE not found with id: " + aceId));

        handleOrderChange(ace, newOrderValue);
        ace.setOrderValue(newOrderValue);

        return aceRepository.save(ace);
    }

    /**
     * Löscht alle ACEs für einen Benutzer
     */
    public void deleteAllAcesForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        aceRepository.deleteByUser(user);
    }

    /**
     * Zählt die ACEs für einen Benutzer
     */
    @Transactional(readOnly = true)
    public long countAcesByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return aceRepository.countByUser(user);
    }

    // Private Hilfsmethoden

    private Integer getNextOrderValue(Long userId) {
        Optional<Integer> maxOrder = aceRepository.findMaxOrderValueByUserId(userId);
        return maxOrder.map(order -> order + 1).orElse(1);
    }

    private void handleOrderChange(Ace ace, Integer newOrderValue) {
        Long userId = ace.getUser().getId();
        Integer currentOrder = ace.getOrderValue();

        if (newOrderValue > currentOrder) {
            // Nach unten verschieben - andere ACEs nach oben
            shiftOrderValues(userId, currentOrder + 1, newOrderValue, -1);
        } else if (newOrderValue < currentOrder) {
            // Nach oben verschieben - andere ACEs nach unten
            shiftOrderValues(userId, newOrderValue, currentOrder - 1, 1);
        }
    }

    private void shiftOrderValues(Long userId, Integer from, Integer to, Integer shift) {
        List<Ace> acesToShift = aceRepository.findByUserIdOrderByOrderValueAsc(userId);

        for (Ace ace : acesToShift) {
            Integer order = ace.getOrderValue();
            if (order >= from && order <= to) {
                ace.setOrderValue(order + shift);
                aceRepository.save(ace);
            }
        }
    }

    private void shiftOrderValues(Long userId, Integer fromOrder, Integer shift) {
        List<Ace> acesToShift = aceRepository.findByUserIdOrderByOrderValueAsc(userId);

        for (Ace ace : acesToShift) {
            if (ace.getOrderValue() >= fromOrder) {
                ace.setOrderValue(ace.getOrderValue() + shift);
                aceRepository.save(ace);
            }
        }
    }

    private void compactOrderValues(Long userId) {
        List<Ace> aces = aceRepository.findByUserIdOrderByOrderValueAsc(userId);

        for (int i = 0; i < aces.size(); i++) {
            Ace ace = aces.get(i);
            if (ace.getOrderValue() != i + 1) {
                ace.setOrderValue(i + 1);
                aceRepository.save(ace);
            }
        }
    }
}
