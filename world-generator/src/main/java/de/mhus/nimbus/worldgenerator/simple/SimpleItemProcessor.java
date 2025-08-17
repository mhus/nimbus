package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Simple Item-Generierung für Gegenstände, Waffen und Ausrüstung.
 * Erstellt Items basierend auf Welttyp und Schwierigkeitsgrad.
 */
@Component
@Slf4j
public class SimpleItemProcessor implements PhaseProcessor {

    private final Random random = new Random();

    private final List<String> WEAPON_TYPES = Arrays.asList(
        "Schwert", "Axt", "Bogen", "Dolch", "Hammer", "Speer",
        "Stab", "Armbrust", "Schild", "Keule"
    );

    private final List<String> ARMOR_TYPES = Arrays.asList(
        "Helm", "Brustpanzer", "Hose", "Stiefel", "Handschuhe",
        "Umhang", "Robe", "Lederrüstung", "Kettenhemd", "Plattenrüstung"
    );

    private final List<String> TOOL_TYPES = Arrays.asList(
        "Spitzhacke", "Axt", "Schaufel", "Hammer", "Säge",
        "Angelrute", "Kochgeschirr", "Seil", "Fackel", "Kompass"
    );

    private final List<String> CONSUMABLE_TYPES = Arrays.asList(
        "Heiltrank", "Manatrank", "Nahrung", "Gift", "Antidot",
        "Stärkungstrank", "Unsichtbarkeitstrank", "Feuerbombe", "Rauchbombe", "Kräuter"
    );

    private final List<String> TREASURE_TYPES = Arrays.asList(
        "Gold", "Silber", "Edelstein", "Kunstwerk", "Antike_Münze",
        "Kristall", "Perle", "Schmuck", "Reliquie", "Magisches_Artefakt"
    );

    private final List<String> MATERIALS = Arrays.asList(
        "Holz", "Eisen", "Stahl", "Silber", "Gold", "Mithril",
        "Leder", "Stoff", "Kristall", "Knochen", "Stein", "Obsidian"
    );

    private final List<String> RARITIES = Arrays.asList(
        "Gewöhnlich", "Ungewöhnlich", "Selten", "Episch", "Legendär"
    );

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Item-Generierung für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        Map<String, Object> parameters = phase.getParameters();
        int seed = (Integer) parameters.getOrDefault("seed", random.nextInt(1000000));
        int itemDensity = (Integer) parameters.getOrDefault("itemDensity", 3); // 1-5 Dichte
        String worldType = (String) parameters.getOrDefault("worldType", "fantasy");

        Random seedRandom = new Random(seed);

        // Simuliere Generierungszeit
        Thread.sleep(1500 + random.nextInt(2500));

        // Generiere verschiedene Item-Kategorien
        generateWeapons(itemDensity, worldType, seedRandom);
        generateArmor(itemDensity, worldType, seedRandom);
        generateTools(itemDensity, worldType, seedRandom);
        generateConsumables(itemDensity, worldType, seedRandom);
        generateTreasures(itemDensity, worldType, seedRandom);

        // Generiere magische Items basierend auf Welttyp
        if ("fantasy".equals(worldType)) {
            generateMagicalItems(itemDensity, seedRandom);
        }

        log.info("Item-Generierung abgeschlossen für Welttyp: {}", worldType);
    }

    private void generateWeapons(int density, String worldType, Random seedRandom) {
        int weaponCount = density * 5 + seedRandom.nextInt(density * 3);

        log.debug("Generiere {} Waffen", weaponCount);

        for (int i = 0; i < weaponCount; i++) {
            String weaponType = WEAPON_TYPES.get(seedRandom.nextInt(WEAPON_TYPES.size()));
            String material = getMaterialForWorldType(worldType, seedRandom);
            String rarity = getRarityByChance(seedRandom);

            int damage = calculateWeaponDamage(weaponType, material, rarity, seedRandom);
            int durability = calculateDurability(material, rarity, seedRandom);
            int value = calculateValue(weaponType, material, rarity, damage, seedRandom);

            String weaponName = generateWeaponName(weaponType, material, rarity, seedRandom);

            log.debug("Waffe {}: {} - Schaden: {}, Haltbarkeit: {}, Wert: {} Gold",
                    i+1, weaponName, damage, durability, value);

            // Spezielle Eigenschaften für seltene Waffen
            if (!"Gewöhnlich".equals(rarity)) {
                generateSpecialWeaponProperties(weaponName, rarity, seedRandom);
            }
        }
    }

    private void generateArmor(int density, String worldType, Random seedRandom) {
        int armorCount = density * 4 + seedRandom.nextInt(density * 2);

        log.debug("Generiere {} Rüstungsteile", armorCount);

        for (int i = 0; i < armorCount; i++) {
            String armorType = ARMOR_TYPES.get(seedRandom.nextInt(ARMOR_TYPES.size()));
            String material = getMaterialForWorldType(worldType, seedRandom);
            String rarity = getRarityByChance(seedRandom);

            int protection = calculateArmorProtection(armorType, material, rarity, seedRandom);
            int durability = calculateDurability(material, rarity, seedRandom);
            int value = calculateValue(armorType, material, rarity, protection, seedRandom);

            String armorName = generateArmorName(armorType, material, rarity, seedRandom);

            log.debug("Rüstung {}: {} - Schutz: {}, Haltbarkeit: {}, Wert: {} Gold",
                    i+1, armorName, protection, durability, value);
        }
    }

    private void generateTools(int density, String worldType, Random seedRandom) {
        int toolCount = density * 3 + seedRandom.nextInt(density * 2);

        log.debug("Generiere {} Werkzeuge", toolCount);

        for (int i = 0; i < toolCount; i++) {
            String toolType = TOOL_TYPES.get(seedRandom.nextInt(TOOL_TYPES.size()));
            String material = getMaterialForWorldType(worldType, seedRandom);
            String rarity = getRarityByChance(seedRandom);

            int efficiency = calculateToolEfficiency(toolType, material, rarity, seedRandom);
            int durability = calculateDurability(material, rarity, seedRandom);
            int value = calculateValue(toolType, material, rarity, efficiency, seedRandom);

            String toolName = generateToolName(toolType, material, rarity, seedRandom);

            log.debug("Werkzeug {}: {} - Effizienz: {}, Haltbarkeit: {}, Wert: {} Gold",
                    i+1, toolName, efficiency, durability, value);
        }
    }

    private void generateConsumables(int density, String worldType, Random seedRandom) {
        int consumableCount = density * 8 + seedRandom.nextInt(density * 5);

        log.debug("Generiere {} Verbrauchsgegenstände", consumableCount);

        for (int i = 0; i < consumableCount; i++) {
            String consumableType = CONSUMABLE_TYPES.get(seedRandom.nextInt(CONSUMABLE_TYPES.size()));
            String rarity = getRarityByChance(seedRandom);

            int potency = calculateConsumablePotency(consumableType, rarity, seedRandom);
            int duration = calculateConsumableDuration(consumableType, rarity, seedRandom);
            int value = calculateConsumableValue(consumableType, rarity, potency, seedRandom);

            String consumableName = generateConsumableName(consumableType, rarity, seedRandom);

            log.debug("Verbrauchsgegenstand {}: {} - Stärke: {}, Dauer: {}s, Wert: {} Gold",
                    i+1, consumableName, potency, duration, value);
        }
    }

    private void generateTreasures(int density, String worldType, Random seedRandom) {
        int treasureCount = density + seedRandom.nextInt(density * 2);

        log.debug("Generiere {} Schätze", treasureCount);

        for (int i = 0; i < treasureCount; i++) {
            String treasureType = TREASURE_TYPES.get(seedRandom.nextInt(TREASURE_TYPES.size()));
            String rarity = getRarityByChance(seedRandom);

            int value = calculateTreasureValue(treasureType, rarity, seedRandom);
            String treasureName = generateTreasureName(treasureType, rarity, seedRandom);

            log.debug("Schatz {}: {} - Wert: {} Gold", i+1, treasureName, value);
        }
    }

    private void generateMagicalItems(int density, Random seedRandom) {
        int magicalCount = density + seedRandom.nextInt(density);

        log.debug("Generiere {} magische Gegenstände", magicalCount);

        String[] magicalTypes = {"Zauberstab", "Amulett", "Ring", "Orb", "Kristall", "Rune"};

        for (int i = 0; i < magicalCount; i++) {
            String magicalType = magicalTypes[seedRandom.nextInt(magicalTypes.length)];
            String rarity = getRarityByChance(seedRandom);

            int magicalPower = calculateMagicalPower(rarity, seedRandom);
            String magicalEffect = generateMagicalEffect(magicalType, seedRandom);
            int value = magicalPower * 50 + seedRandom.nextInt(magicalPower * 20);

            String magicalName = generateMagicalItemName(magicalType, magicalEffect, rarity, seedRandom);

            log.debug("Magischer Gegenstand {}: {} - Kraft: {}, Effekt: {}, Wert: {} Gold",
                    i+1, magicalName, magicalPower, magicalEffect, value);
        }
    }

    private String getMaterialForWorldType(String worldType, Random seedRandom) {
        switch (worldType) {
            case "fantasy":
                return MATERIALS.get(seedRandom.nextInt(MATERIALS.size()));
            case "medieval":
                String[] medievalMaterials = {"Holz", "Eisen", "Stahl", "Leder", "Stoff"};
                return medievalMaterials[seedRandom.nextInt(medievalMaterials.length)];
            case "primitive":
                String[] primitiveMaterials = {"Holz", "Stein", "Knochen", "Leder"};
                return primitiveMaterials[seedRandom.nextInt(primitiveMaterials.length)];
            default:
                return MATERIALS.get(seedRandom.nextInt(MATERIALS.size()));
        }
    }

    private String getRarityByChance(Random seedRandom) {
        int roll = seedRandom.nextInt(100);
        if (roll < 50) return "Gewöhnlich";     // 50%
        if (roll < 75) return "Ungewöhnlich";   // 25%
        if (roll < 90) return "Selten";         // 15%
        if (roll < 98) return "Episch";         // 8%
        return "Legendär";                      // 2%
    }

    private int calculateWeaponDamage(String weaponType, String material, String rarity, Random seedRandom) {
        int baseDamage = getBaseWeaponDamage(weaponType);
        float materialMultiplier = getMaterialMultiplier(material);
        float rarityMultiplier = getRarityMultiplier(rarity);

        return (int) (baseDamage * materialMultiplier * rarityMultiplier * (0.8f + seedRandom.nextFloat() * 0.4f));
    }

    private int getBaseWeaponDamage(String weaponType) {
        switch (weaponType) {
            case "Schwert": return 25;
            case "Axt": return 30;
            case "Bogen": return 20;
            case "Dolch": return 15;
            case "Hammer": return 35;
            case "Speer": return 22;
            case "Stab": return 12;
            case "Armbrust": return 28;
            case "Schild": return 5; // Defensiv
            case "Keule": return 18;
            default: return 20;
        }
    }

    private float getMaterialMultiplier(String material) {
        switch (material) {
            case "Holz": return 0.7f;
            case "Eisen": return 1.0f;
            case "Stahl": return 1.3f;
            case "Silber": return 1.1f;
            case "Gold": return 1.2f;
            case "Mithril": return 2.0f;
            case "Kristall": return 1.5f;
            case "Obsidian": return 1.4f;
            default: return 1.0f;
        }
    }

    private float getRarityMultiplier(String rarity) {
        switch (rarity) {
            case "Gewöhnlich": return 1.0f;
            case "Ungewöhnlich": return 1.2f;
            case "Selten": return 1.5f;
            case "Episch": return 2.0f;
            case "Legendär": return 3.0f;
            default: return 1.0f;
        }
    }

    private int calculateArmorProtection(String armorType, String material, String rarity, Random seedRandom) {
        int baseProtection = getBaseArmorProtection(armorType);
        float materialMultiplier = getMaterialMultiplier(material);
        float rarityMultiplier = getRarityMultiplier(rarity);

        return (int) (baseProtection * materialMultiplier * rarityMultiplier * (0.8f + seedRandom.nextFloat() * 0.4f));
    }

    private int getBaseArmorProtection(String armorType) {
        switch (armorType) {
            case "Helm": return 8;
            case "Brustpanzer": return 25;
            case "Hose": return 12;
            case "Stiefel": return 6;
            case "Handschuhe": return 4;
            case "Umhang": return 3;
            case "Robe": return 5;
            case "Lederrüstung": return 15;
            case "Kettenhemd": return 20;
            case "Plattenrüstung": return 35;
            default: return 10;
        }
    }

    private int calculateDurability(String material, String rarity, Random seedRandom) {
        int baseDurability = 100;
        float materialMultiplier = getMaterialMultiplier(material);
        float rarityMultiplier = getRarityMultiplier(rarity);

        return (int) (baseDurability * materialMultiplier * rarityMultiplier * (0.8f + seedRandom.nextFloat() * 0.4f));
    }

    private int calculateValue(String itemType, String material, String rarity, int stat, Random seedRandom) {
        int baseValue = stat * 2;
        float materialValue = getMaterialValueMultiplier(material);
        float rarityValue = getRarityMultiplier(rarity);

        return (int) (baseValue * materialValue * rarityValue * (0.8f + seedRandom.nextFloat() * 0.4f));
    }

    private float getMaterialValueMultiplier(String material) {
        switch (material) {
            case "Gold": return 10.0f;
            case "Silber": return 5.0f;
            case "Mithril": return 20.0f;
            case "Kristall": return 8.0f;
            case "Stahl": return 3.0f;
            case "Eisen": return 1.5f;
            case "Holz": return 0.5f;
            case "Leder": return 1.0f;
            default: return 2.0f;
        }
    }

    private String generateWeaponName(String weaponType, String material, String rarity, Random seedRandom) {
        String[] prefixes = {"Verfluchtes", "Gesegnetes", "Altes", "Glänzendes", "Scharfes", "Mächtiges"};
        String[] suffixes = {"der Macht", "des Kriegers", "der Ehre", "des Lichts", "der Dunkelheit", "der Zerstörung"};

        if ("Gewöhnlich".equals(rarity)) {
            return material + "-" + weaponType;
        } else {
            String prefix = prefixes[seedRandom.nextInt(prefixes.length)];
            String suffix = suffixes[seedRandom.nextInt(suffixes.length)];
            return prefix + " " + material + "-" + weaponType + " " + suffix;
        }
    }

    // Ähnliche Methoden für andere Item-Typen...
    private String generateArmorName(String armorType, String material, String rarity, Random seedRandom) {
        if ("Gewöhnlich".equals(rarity)) {
            return material + "-" + armorType;
        } else {
            String[] adjectives = {"Verstärkt", "Verzaubert", "Magisch", "Königlich", "Legendär"};
            String adjective = adjectives[seedRandom.nextInt(adjectives.length)];
            return adjective + "er " + material + "-" + armorType;
        }
    }

    private String generateToolName(String toolType, String material, String rarity, Random seedRandom) {
        return material + "-" + toolType + (!"Gewöhnlich".equals(rarity) ? " (" + rarity + ")" : "");
    }

    private String generateConsumableName(String consumableType, String rarity, Random seedRandom) {
        String[] qualifiers = {"Schwach", "Normal", "Stark", "Sehr Stark", "Extrem"};
        String qualifier = qualifiers[Math.min(RARITIES.indexOf(rarity), qualifiers.length - 1)];
        return qualifier + "er " + consumableType;
    }

    private String generateTreasureName(String treasureType, String rarity, Random seedRandom) {
        return rarity + "er " + treasureType;
    }

    private String generateMagicalItemName(String magicalType, String effect, String rarity, Random seedRandom) {
        return rarity + "er " + magicalType + " des " + effect;
    }

    // Zusätzliche Berechnungsmethoden...
    private int calculateToolEfficiency(String toolType, String material, String rarity, Random seedRandom) {
        return calculateWeaponDamage(toolType, material, rarity, seedRandom) / 2;
    }

    private int calculateConsumablePotency(String consumableType, String rarity, Random seedRandom) {
        int basePotency = 50;
        return (int) (basePotency * getRarityMultiplier(rarity) * (0.8f + seedRandom.nextFloat() * 0.4f));
    }

    private int calculateConsumableDuration(String consumableType, String rarity, Random seedRandom) {
        int baseDuration = 30; // Sekunden
        return (int) (baseDuration * getRarityMultiplier(rarity) * (0.5f + seedRandom.nextFloat() * 1.0f));
    }

    private int calculateConsumableValue(String consumableType, String rarity, int potency, Random seedRandom) {
        return potency + (int) (getRarityMultiplier(rarity) * 10);
    }

    private int calculateTreasureValue(String treasureType, String rarity, Random seedRandom) {
        int baseValue = 100;
        return (int) (baseValue * getRarityMultiplier(rarity) * (0.5f + seedRandom.nextFloat() * 2.0f));
    }

    private int calculateMagicalPower(String rarity, Random seedRandom) {
        return (int) (50 * getRarityMultiplier(rarity) * (0.8f + seedRandom.nextFloat() * 0.4f));
    }

    private String generateMagicalEffect(String magicalType, Random seedRandom) {
        String[] effects = {"Heilung", "Schutz", "Stärke", "Geschwindigkeit", "Weisheit",
                          "Feuer", "Eis", "Blitz", "Unsichtbarkeit", "Teleportation"};
        return effects[seedRandom.nextInt(effects.length)];
    }

    private void generateSpecialWeaponProperties(String weaponName, String rarity, Random seedRandom) {
        String[] properties = {"Flammend", "Eisig", "Blitzend", "Giftig", "Heilend", "Durchdringend"};
        String property = properties[seedRandom.nextInt(properties.length)];
        log.debug("Spezielle Eigenschaft für {}: {}", weaponName, property);
    }

    @Override
    public String getPhaseType() {
        return "ITEM_GENERATION";
    }

    @Override
    public String getProcessorName() {
        return "SimpleItemProcessor";
    }
}
