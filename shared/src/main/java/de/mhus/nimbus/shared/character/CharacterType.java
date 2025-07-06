package de.mhus.nimbus.shared.character;

/**
 * Enumeration of all character types available in the Nimbus platform.
 * Defines the basic categories of entities that can exist in the world.
 */
public enum CharacterType {

    /**
     * Player character controlled by a user
     */
    PLAYER("Player", "A character controlled by a human player"),

    /**
     * Non-Player Character (NPC) with AI behavior
     */
    NPC("NPC", "A non-player character with automated behavior"),

    /**
     * Flora - plant life and vegetation
     */
    FLORA("Flora", "Plant life including trees, bushes, flowers, and other vegetation"),

    /**
     * Fauna - animal life
     */
    FAUNA("Fauna", "Animal life including creatures, pets, and wildlife"),

    /**
     * Animal creatures and beasts
     */
    ANIMAL("Animal", "Wild animals, domesticated creatures, and beasts"),

    /**
     * Hostile monsters and dangerous creatures
     */
    MONSTER("Monster", "Hostile creatures, demons, and dangerous beings"),

    /**
     * Artificial constructs and mechanical entities
     */
    CONSTRUCT("Construct", "Artificial beings, robots, and mechanical entities"),

    /**
     * Environmental objects that can interact
     */
    OBJECT("Object", "Interactive environmental objects and items"),

    /**
     * Spiritual or ethereal entities
     */
    SPIRIT("Spirit", "Ghostly, spiritual, or otherworldly beings");

    private final String displayName;
    private final String description;

    CharacterType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
