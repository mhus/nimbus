package de.mhus.nimbus.region.character;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;

@Service
@Validated
public class RCharacterService {

    private final RCharacterRepository repository;

    public RCharacterService(RCharacterRepository repository) {
        this.repository = repository;
    }

    public RCharacter createCharacter(String userId, String name, String display) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId blank");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name blank");
        if (repository.existsByUserIdAndName(userId, name)) {
            throw new IllegalArgumentException("Character name already exists for user: " + name);
        }
        RCharacter c = new RCharacter(userId, name, display != null ? display : name);
        return repository.save(c);
    }

    public Optional<RCharacter> getCharacter(String userId, String name) {
        return repository.findByUserIdAndName(userId, name);
    }

    public List<RCharacter> listCharacters(String userId) {
        return repository.findByUserId(userId);
    }

    public RCharacter updateDisplay(String userId, String name, String display) {
        RCharacter c = repository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        if (display != null && !display.isBlank()) c.setDisplay(display);
        return repository.save(c);
    }

    public RCharacter addBackpackItem(String userId, String name, String key, ItemInfo item) {
        RCharacter c = repository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.putBackpackItem(key, item);
        return repository.save(c);
    }

    public RCharacter removeBackpackItem(String userId, String name, String key) {
        RCharacter c = repository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.removeBackpackItem(key);
        return repository.save(c);
    }

    public RCharacter wearItem(String userId, String name, Integer slot, ItemInfo item) {
        RCharacter c = repository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.putWearingItem(slot, item);
        return repository.save(c);
    }

    public RCharacter removeWearingItem(String userId, String name, Integer slot) {
        RCharacter c = repository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.removeWearingItem(slot);
        return repository.save(c);
    }

    public RCharacter setSkill(String userId, String name, String skill, int level) {
        RCharacter c = repository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.setSkill(skill, level);
        return repository.save(c);
    }

    public RCharacter incrementSkill(String userId, String name, String skill, int delta) {
        RCharacter c = repository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.incrementSkill(skill, delta);
        return repository.save(c);
    }

    public void deleteCharacter(String userId, String name) {
        RCharacter c = repository.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        repository.delete(c);
    }
}

