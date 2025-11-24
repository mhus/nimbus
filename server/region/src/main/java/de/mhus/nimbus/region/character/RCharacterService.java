package de.mhus.nimbus.region.character;

import de.mhus.nimbus.shared.dto.region.RegionItemInfo; // neuer Import
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

    public RCharacter createCharacter(String userId, String regionId, String name, String display) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("userId blank");
        if (regionId == null || regionId.isBlank()) throw new IllegalArgumentException("regionId blank");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name blank");
        if (repository.existsByUserIdAndRegionIdAndName(userId, regionId, name)) {
            throw new IllegalArgumentException("Character name already exists for user/region: " + name);
        }
        RCharacter c = new RCharacter(userId, regionId, name, display != null ? display : name);
        return repository.save(c);
    }

    public Optional<RCharacter> getCharacter(String userId, String regionId, String name) {
        return repository.findByUserIdAndRegionIdAndName(userId, regionId, name);
    }

    public List<RCharacter> listCharacters(String userId, String regionId) {
        return repository.findByUserIdAndRegionId(userId, regionId);
    }

    public RCharacter updateDisplay(String userId, String regionId, String name, String display) {
        RCharacter c = repository.findByUserIdAndRegionIdAndName(userId, regionId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        if (display != null && !display.isBlank()) c.setDisplay(display);
        return repository.save(c);
    }

    public RCharacter addBackpackItem(String userId, String regionId, String name, String key, RegionItemInfo item) {
        RCharacter c = repository.findByUserIdAndRegionIdAndName(userId, regionId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.putBackpackItem(key, item);
        return repository.save(c);
    }

    public RCharacter removeBackpackItem(String userId, String regionId, String name, String key) {
        RCharacter c = repository.findByUserIdAndRegionIdAndName(userId, regionId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.removeBackpackItem(key);
        return repository.save(c);
    }

    public RCharacter wearItem(String userId, String regionId, String name, Integer slot, RegionItemInfo item) {
        RCharacter c = repository.findByUserIdAndRegionIdAndName(userId, regionId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.putWearingItem(slot, item);
        return repository.save(c);
    }

    public RCharacter removeWearingItem(String userId, String regionId, String name, Integer slot) {
        RCharacter c = repository.findByUserIdAndRegionIdAndName(userId, regionId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.removeWearingItem(slot);
        return repository.save(c);
    }

    public RCharacter setSkill(String userId, String regionId, String name, String skill, int level) {
        RCharacter c = repository.findByUserIdAndRegionIdAndName(userId, regionId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.setSkill(skill, level);
        return repository.save(c);
    }

    public RCharacter incrementSkill(String userId, String regionId, String name, String skill, int delta) {
        RCharacter c = repository.findByUserIdAndRegionIdAndName(userId, regionId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        c.incrementSkill(skill, delta);
        return repository.save(c);
    }

    public void deleteCharacter(String userId, String regionId, String name) {
        RCharacter c = repository.findByUserIdAndRegionIdAndName(userId, regionId, name)
                .orElseThrow(() -> new IllegalArgumentException("Character not found"));
        repository.delete(c);
    }

    public Optional<RCharacter> getById(String id) {
        return repository.findById(id);
    }

    public Optional<RCharacter> getByIdAndRegion(String id, String regionId) {
        return repository.findByIdAndRegionId(id, regionId);
    }
}
