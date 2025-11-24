package de.mhus.nimbus.region.character;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "characters")
@CompoundIndex(def = "{userId:1, name:1}", unique = true)
@Data
public class RCharacter {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String name;      // eindeutiger Name pro userId
    private String display;   // Anzeige-Name

    @CreatedDate
    private Instant createdAt;

    // Inventar (beliebige Slots -> ItemInfo)
    private Map<String, ItemInfo> backpack;

    // Ausgerüstete Items (z.B. Slotnummer -> ItemInfo)
    private Map<Integer, ItemInfo> wearing;

    // Skills (Skill-Name -> Level)
    private Map<String, Integer> skills;

    public RCharacter() { }
    public RCharacter(String userId, String name, String display) {
        this.userId = userId;
        this.name = name;
        this.display = display;
    }

    public Map<String, ItemInfo> getBackpack() { if (backpack == null) backpack = new HashMap<>(); return backpack; }
    public Map<Integer, ItemInfo> getWearing() { if (wearing == null) wearing = new HashMap<>(); return wearing; }
    public Map<String, Integer> getSkills() { if (skills == null) skills = new HashMap<>(); return skills; }

    public void putBackpackItem(String key, ItemInfo item) { getBackpack().put(key, item); }
    public ItemInfo removeBackpackItem(String key) { return getBackpack().remove(key); }

    public void putWearingItem(Integer slot, ItemInfo item) { getWearing().put(slot, item); }
    public ItemInfo removeWearingItem(Integer slot) { return getWearing().remove(slot); }

    public void setSkill(String skill, int level) {
        if (level < 0) level = 0;
        getSkills().put(skill, level);
    }
    public int incrementSkill(String skill, int delta) {
        int current = getSkills().getOrDefault(skill, 0);
        int next = Math.max(0, current + delta);
        getSkills().put(skill, next);
        return next;
    }
}

