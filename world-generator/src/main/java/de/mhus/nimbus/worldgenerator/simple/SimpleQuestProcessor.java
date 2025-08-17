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
 * Simple Quest-Generierung für Aufgaben und NPCs.
 * Erstellt Quests, NPCs und Storylines für die Welt.
 */
@Component
@Slf4j
public class SimpleQuestProcessor implements PhaseProcessor {

    private final Random random = new Random();

    private final List<String> QUEST_TYPES = Arrays.asList(
        "Sammeln", "Töten", "Eskortieren", "Lieferung", "Erkundung",
        "Rettung", "Handwerk", "Diplomatie", "Rätsel", "Reinigung"
    );

    private final List<String> NPC_TYPES = Arrays.asList(
        "Dorfbewohner", "Händler", "Wächter", "Magier", "Priester",
        "Schmied", "Bauer", "Adliger", "Einsiedler", "Bandit"
    );

    private final List<String> QUEST_DIFFICULTIES = Arrays.asList(
        "Einfach", "Normal", "Schwer", "Sehr_Schwer", "Episch"
    );

    private final List<String> STORY_THEMES = Arrays.asList(
        "Gute_vs_Böse", "Verlust_und_Wiederfindung", "Heldentum", "Rache",
        "Liebe", "Entdeckung", "Überleben", "Macht_und_Korruption", "Opfer", "Erlösung"
    );

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Quest-Generierung für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        Map<String, Object> parameters = phase.getParameters();
        int seed = (Integer) parameters.getOrDefault("seed", random.nextInt(1000000));
        int questDensity = (Integer) parameters.getOrDefault("questDensity", 3); // 1-5 Dichte
        String worldTheme = (String) parameters.getOrDefault("worldTheme", "fantasy");

        Random seedRandom = new Random(seed);

        // Simuliere Generierungszeit
        Thread.sleep(1800 + random.nextInt(2200));

        // Generiere NPCs zuerst
        int npcCount = questDensity * 8 + seedRandom.nextInt(questDensity * 5);
        generateNPCs(npcCount, worldTheme, seedRandom);

        // Generiere Quests basierend auf NPCs
        int questCount = questDensity * 6 + seedRandom.nextInt(questDensity * 4);
        generateQuests(questCount, npcCount, worldTheme, seedRandom);

        // Generiere Haupt-Storylines
        int storylineCount = 1 + seedRandom.nextInt(3); // 1-3 Hauptstorys
        generateStorylines(storylineCount, worldTheme, seedRandom);

        log.info("Quest-Generierung abgeschlossen - {} NPCs, {} Quests, {} Storylines",
                npcCount, questCount, storylineCount);
    }

    private void generateNPCs(int npcCount, String worldTheme, Random seedRandom) {
        log.debug("Generiere {} NPCs", npcCount);

        for (int i = 0; i < npcCount; i++) {
            generateNPC(i + 1, worldTheme, seedRandom);
        }
    }

    private void generateNPC(int npcId, String worldTheme, Random seedRandom) {
        String npcType = NPC_TYPES.get(seedRandom.nextInt(NPC_TYPES.size()));
        String name = generateNPCName(npcType, seedRandom);
        int level = 1 + seedRandom.nextInt(50); // Level 1-50
        String personality = generatePersonality(seedRandom);
        String background = generateBackground(npcType, worldTheme, seedRandom);

        // Position des NPCs
        int x = seedRandom.nextInt(1000);
        int y = seedRandom.nextInt(1000);

        log.debug("NPC {}: {} '{}' (Level {}) - Position: ({}, {}) - {}, {}",
                npcId, npcType, name, level, x, y, personality, background);

        // Generiere NPC-spezifische Eigenschaften
        generateNPCProperties(npcId, name, npcType, level, seedRandom);
    }

    private String generateNPCName(String npcType, Random seedRandom) {
        String[] firstNames = {"Aldric", "Brianna", "Cedric", "Diana", "Eldrin", "Fiona",
                             "Gareth", "Helena", "Ivan", "Jenna", "Kael", "Luna"};
        String[] lastNames = {"Eisenfaust", "Goldherz", "Steinbrecher", "Windläufer",
                            "Feuerblick", "Mondschein", "Sternenwanderer", "Drachenherz"};

        String firstName = firstNames[seedRandom.nextInt(firstNames.length)];
        String lastName = lastNames[seedRandom.nextInt(lastNames.length)];

        return firstName + " " + lastName;
    }

    private String generatePersonality(Random seedRandom) {
        String[] personalities = {"Freundlich", "Mürrisch", "Neugierig", "Vorsichtig", "Hilfsbereit",
                                "Gierig", "Ehrlich", "Geheimnisvoll", "Fröhlich", "Melancholisch"};
        return personalities[seedRandom.nextInt(personalities.length)];
    }

    private String generateBackground(String npcType, String worldTheme, Random seedRandom) {
        switch (npcType) {
            case "Dorfbewohner":
                String[] villagerBG = {"Einfacher Bürger", "Ehemaliger Abenteurer", "Lokaler Experte", "Familienmensch"};
                return villagerBG[seedRandom.nextInt(villagerBG.length)];
            case "Händler":
                String[] merchantBG = {"Weitgereister Kaufmann", "Lokaler Geschäftsmann", "Schwarzmarkthändler", "Familienunternehmen"};
                return merchantBG[seedRandom.nextInt(merchantBG.length)];
            case "Magier":
                String[] mageBG = {"Gelehrter", "Einsiedler", "Hofmagier", "Gefallener Zauberer"};
                return mageBG[seedRandom.nextInt(mageBG.length)];
            case "Bandit":
                String[] banditBG = {"Verzweifelter Dieb", "Organisiertes Verbrechen", "Ehemaliger Soldat", "Robin Hood-Typ"};
                return banditBG[seedRandom.nextInt(banditBG.length)];
            default:
                return "Gewöhnlicher Hintergrund";
        }
    }

    private void generateNPCProperties(int npcId, String name, String npcType, int level, Random seedRandom) {
        // Generiere Fähigkeiten basierend auf NPC-Typ
        switch (npcType) {
            case "Händler":
                int merchantSkill = 50 + seedRandom.nextInt(50);
                String[] goods = {"Waffen", "Rüstung", "Tränke", "Werkzeuge", "Edelsteine"};
                String speciality = goods[seedRandom.nextInt(goods.length)];
                log.debug("  {} handelt mit {} (Skill: {})", name, speciality, merchantSkill);
                break;
            case "Schmied":
                int smithSkill = 60 + seedRandom.nextInt(40);
                String[] smithTypes = {"Waffen", "Rüstung", "Werkzeuge", "Hufeisen", "Kunstwerke"};
                String smithSpecialty = smithTypes[seedRandom.nextInt(smithTypes.length)];
                log.debug("  {} schmiedet {} (Skill: {})", name, smithSpecialty, smithSkill);
                break;
            case "Magier":
                String[] schools = {"Feuer", "Wasser", "Erde", "Luft", "Licht", "Dunkel", "Heilung", "Nekromantie"};
                String magicSchool = schools[seedRandom.nextInt(schools.length)];
                int manaPool = level * 10 + seedRandom.nextInt(level * 5);
                log.debug("  {} beherrscht {}-Magie (Mana: {})", name, magicSchool, manaPool);
                break;
        }

        // Generiere Items, die der NPC besitzt/verkauft
        if (seedRandom.nextBoolean()) {
            String item = generateNPCItem(npcType, seedRandom);
            log.debug("  {} besitzt: {}", name, item);
        }
    }

    private String generateNPCItem(String npcType, Random seedRandom) {
        switch (npcType) {
            case "Händler":
                String[] merchantItems = {"Seltener Edelstein", "Magischer Trank", "Alte Karte", "Exotische Ware"};
                return merchantItems[seedRandom.nextInt(merchantItems.length)];
            case "Magier":
                String[] mageItems = {"Zauberbuch", "Magischer Stab", "Kristallkugel", "Alchemistische Komponenten"};
                return mageItems[seedRandom.nextInt(mageItems.length)];
            case "Wächter":
                String[] guardItems = {"Verzauberte Waffe", "Magische Rüstung", "Schild der Ehre"};
                return guardItems[seedRandom.nextInt(guardItems.length)];
            default:
                String[] commonItems = {"Familienerbe", "Altes Werkzeug", "Persönlicher Brief", "Glücksbringer"};
                return commonItems[seedRandom.nextInt(commonItems.length)];
        }
    }

    private void generateQuests(int questCount, int npcCount, String worldTheme, Random seedRandom) {
        log.debug("Generiere {} Quests", questCount);

        for (int i = 0; i < questCount; i++) {
            generateQuest(i + 1, npcCount, worldTheme, seedRandom);
        }
    }

    private void generateQuest(int questId, int npcCount, String worldTheme, Random seedRandom) {
        String questType = QUEST_TYPES.get(seedRandom.nextInt(QUEST_TYPES.size()));
        String difficulty = QUEST_DIFFICULTIES.get(seedRandom.nextInt(QUEST_DIFFICULTIES.size()));
        String questName = generateQuestName(questType, difficulty, seedRandom);

        int questGiverNPC = 1 + seedRandom.nextInt(npcCount);
        int experienceReward = calculateExperienceReward(difficulty, seedRandom);
        int goldReward = calculateGoldReward(difficulty, seedRandom);

        log.debug("Quest {}: '{}' ({}) - Geber: NPC{}, Belohnung: {}XP + {}G",
                questId, questName, difficulty, questGiverNPC, experienceReward, goldReward);

        // Generiere Quest-Details
        generateQuestDetails(questId, questType, difficulty, seedRandom);

        // Generiere mögliche Questketten
        if (seedRandom.nextInt(100) < 30) { // 30% Chance für Folgequest
            generateQuestChain(questId, questType, difficulty, seedRandom);
        }
    }

    private String generateQuestName(String questType, String difficulty, Random seedRandom) {
        String[] prefixes = {"Die", "Das", "Der"};
        String prefix = prefixes[seedRandom.nextInt(prefixes.length)];

        switch (questType) {
            case "Sammeln":
                String[] collectNames = {"verlorenen Kräuter", "seltenen Erze", "magischen Komponenten"};
                return prefix + " " + collectNames[seedRandom.nextInt(collectNames.length)];
            case "Töten":
                String[] killNames = {"gefährlichen Bestien", "Banditenanführer", "untoten Kreaturen"};
                return "Vernichte " + killNames[seedRandom.nextInt(killNames.length)];
            case "Eskortieren":
                String[] escortNames = {"Karawane", "wichtige Person", "Pilgergruppe"};
                return "Eskortiere " + escortNames[seedRandom.nextInt(escortNames.length)];
            case "Lieferung":
                String[] deliveryNames = {"wichtige Nachricht", "geheime Pläne", "wertvolle Fracht"};
                return "Liefere " + deliveryNames[seedRandom.nextInt(deliveryNames.length)];
            case "Erkundung":
                String[] exploreNames = {"verlorene Ruinen", "unbekannte Höhle", "geheimnisvolle Insel"};
                return "Erkunde " + exploreNames[seedRandom.nextInt(exploreNames.length)];
            case "Rettung":
                String[] rescueNames = {"entführte Person", "gefangenen Helden", "verschollene Expedition"};
                return "Rette " + rescueNames[seedRandom.nextInt(rescueNames.length)];
            default:
                return "Geheimnisvoll" + "e Quest";
        }
    }

    private void generateQuestDetails(int questId, String questType, String difficulty, Random seedRandom) {
        int timeLimit = getQuestTimeLimit(difficulty, seedRandom);
        String location = generateQuestLocation(questType, seedRandom);
        String[] obstacles = generateQuestObstacles(questType, difficulty, seedRandom);

        log.debug("  Quest {} Details - Ort: {}, Zeitlimit: {}h, Hindernisse: {}",
                questId, location, timeLimit, String.join(", ", obstacles));
    }

    private int getQuestTimeLimit(String difficulty, Random seedRandom) {
        switch (difficulty) {
            case "Einfach": return 24 + seedRandom.nextInt(24); // 1-2 Tage
            case "Normal": return 48 + seedRandom.nextInt(72); // 2-5 Tage
            case "Schwer": return 72 + seedRandom.nextInt(168); // 3-10 Tage
            case "Sehr_Schwer": return 168 + seedRandom.nextInt(336); // 1-3 Wochen
            case "Episch": return 720 + seedRandom.nextInt(1440); // 1-2 Monate
            default: return 72;
        }
    }

    private String generateQuestLocation(String questType, Random seedRandom) {
        String[] locations = {"Düsterer Wald", "Verlassene Höhle", "Antike Ruinen", "Gefährlicher Sumpf",
                            "Hohe Berge", "Tiefe See", "Wüstenstadt", "Eisige Tundra", "Vulkanregion", "Mystischer Turm"};
        return locations[seedRandom.nextInt(locations.length)];
    }

    private String[] generateQuestObstacles(String questType, String difficulty, Random seedRandom) {
        String[] commonObstacles = {"Gefährliche Kreaturen", "Umweltgefahren", "Banditen", "Magische Fallen"};
        String[] hardObstacles = {"Mächtige Monster", "Zeitdruck", "Politische Intrigen", "Rivalisierende Abenteurer"};
        String[] epicObstacles = {"Götterfluch", "Realitätsverzerrung", "Dämonenfürst", "Weltbedrohung"};

        int obstacleCount = 1 + seedRandom.nextInt(3);
        String[] obstacles = new String[obstacleCount];

        for (int i = 0; i < obstacleCount; i++) {
            switch (difficulty) {
                case "Einfach":
                case "Normal":
                    obstacles[i] = commonObstacles[seedRandom.nextInt(commonObstacles.length)];
                    break;
                case "Schwer":
                case "Sehr_Schwer":
                    if (seedRandom.nextBoolean()) {
                        obstacles[i] = hardObstacles[seedRandom.nextInt(hardObstacles.length)];
                    } else {
                        obstacles[i] = commonObstacles[seedRandom.nextInt(commonObstacles.length)];
                    }
                    break;
                case "Episch":
                    obstacles[i] = epicObstacles[seedRandom.nextInt(epicObstacles.length)];
                    break;
            }
        }

        return obstacles;
    }

    private int calculateExperienceReward(String difficulty, Random seedRandom) {
        switch (difficulty) {
            case "Einfach": return 100 + seedRandom.nextInt(200);
            case "Normal": return 300 + seedRandom.nextInt(400);
            case "Schwer": return 700 + seedRandom.nextInt(600);
            case "Sehr_Schwer": return 1300 + seedRandom.nextInt(1000);
            case "Episch": return 2500 + seedRandom.nextInt(2500);
            default: return 500;
        }
    }

    private int calculateGoldReward(String difficulty, Random seedRandom) {
        switch (difficulty) {
            case "Einfach": return 10 + seedRandom.nextInt(40);
            case "Normal": return 50 + seedRandom.nextInt(100);
            case "Schwer": return 150 + seedRandom.nextInt(200);
            case "Sehr_Schwer": return 350 + seedRandom.nextInt(400);
            case "Episch": return 750 + seedRandom.nextInt(1000);
            default: return 100;
        }
    }

    private void generateQuestChain(int originalQuestId, String questType, String difficulty, Random seedRandom) {
        int chainLength = 2 + seedRandom.nextInt(4); // 2-5 Quests in der Kette
        log.debug("  Quest {} startet Kette mit {} Folgequest(s)", originalQuestId, chainLength - 1);

        for (int i = 1; i < chainLength; i++) {
            String nextDifficulty = escalateDifficulty(difficulty, i, seedRandom);
            log.debug("    Folgequest {}.{} - Schwierigkeit: {}", originalQuestId, i, nextDifficulty);
        }
    }

    private String escalateDifficulty(String baseDifficulty, int step, Random seedRandom) {
        int currentIndex = QUEST_DIFFICULTIES.indexOf(baseDifficulty);
        int newIndex = Math.min(currentIndex + step, QUEST_DIFFICULTIES.size() - 1);
        return QUEST_DIFFICULTIES.get(newIndex);
    }

    private void generateStorylines(int storylineCount, String worldTheme, Random seedRandom) {
        log.debug("Generiere {} Hauptstorylines", storylineCount);

        for (int i = 0; i < storylineCount; i++) {
            generateStoryline(i + 1, worldTheme, seedRandom);
        }
    }

    private void generateStoryline(int storylineId, String worldTheme, Random seedRandom) {
        String theme = STORY_THEMES.get(seedRandom.nextInt(STORY_THEMES.size()));
        String title = generateStorylineTitle(theme, worldTheme, seedRandom);
        int chapterCount = 3 + seedRandom.nextInt(7); // 3-9 Kapitel

        log.debug("Storyline {}: '{}' ({}), {} Kapitel", storylineId, title, theme, chapterCount);

        // Generiere Kapitel
        for (int chapter = 1; chapter <= chapterCount; chapter++) {
            generateStoryChapter(storylineId, chapter, title, theme, seedRandom);
        }

        // Generiere Endboss/Finale
        generateStorylineFinale(storylineId, title, theme, seedRandom);
    }

    private String generateStorylineTitle(String theme, String worldTheme, Random seedRandom) {
        switch (theme) {
            case "Gute_vs_Böse":
                String[] goodEvil = {"Schatten über dem Land", "Das Letzte Licht", "Krieg der Götter"};
                return goodEvil[seedRandom.nextInt(goodEvil.length)];
            case "Verlust_und_Wiederfindung":
                String[] lossRecovery = {"Die Verlorene Heimat", "Suche nach dem Erbe", "Weg der Erinnerung"};
                return lossRecovery[seedRandom.nextInt(lossRecovery.length)];
            case "Heldentum":
                String[] heroism = {"Aufstieg des Helden", "Ruf des Schicksals", "Legende der Tapferen"};
                return heroism[seedRandom.nextInt(heroism.length)];
            case "Macht_und_Korruption":
                String[] power = {"Thron der Lügen", "Preis der Macht", "Fall des Königs"};
                return power[seedRandom.nextInt(power.length)];
            default:
                return "Das Große Abenteuer";
        }
    }

    private void generateStoryChapter(int storylineId, int chapter, String title, String theme, Random seedRandom) {
        String chapterTitle = generateChapterTitle(chapter, theme, seedRandom);
        String objective = generateChapterObjective(chapter, theme, seedRandom);

        log.debug("  Kapitel {}.{}: '{}' - Ziel: {}", storylineId, chapter, chapterTitle, objective);
    }

    private String generateChapterTitle(int chapter, String theme, Random seedRandom) {
        String[] titles = {"Erwachen", "Erste Schritte", "Verbündete finden", "Prüfung", "Enthüllung",
                         "Konfrontation", "Verrat", "Rache", "Finale"};
        if (chapter <= titles.length) {
            return titles[chapter - 1];
        } else {
            return "Epische Herausforderung " + (chapter - titles.length);
        }
    }

    private String generateChapterObjective(int chapter, String theme, Random seedRandom) {
        String[] objectives = {"Sammle Informationen", "Finde Verbündete", "Überwinde Hindernisse",
                             "Besiege Gegner", "Löse Rätsel", "Schütze Unschuldige", "Entdecke Geheimnisse"};
        return objectives[seedRandom.nextInt(objectives.length)];
    }

    private void generateStorylineFinale(int storylineId, String title, String theme, Random seedRandom) {
        String finalBoss = generateFinalBoss(theme, seedRandom);
        String reward = generateStorylineReward(theme, seedRandom);

        log.debug("  Storyline {} Finale - Endboss: {}, Belohnung: {}", storylineId, finalBoss, reward);
    }

    private String generateFinalBoss(String theme, Random seedRandom) {
        switch (theme) {
            case "Gute_vs_Böse":
                String[] evilBosses = {"Dämonenlord", "Dunkler Magier", "Gefallener Paladin", "Schatten-König"};
                return evilBosses[seedRandom.nextInt(evilBosses.length)];
            case "Macht_und_Korruption":
                String[] powerBosses = {"Tyrannischer Kaiser", "Korrupter Kanzler", "Verräterischer General"};
                return powerBosses[seedRandom.nextInt(powerBosses.length)];
            case "Rache":
                String[] revengeBosses = {"Rächender Geist", "Verratener Freund", "Gefallener Held"};
                return revengeBosses[seedRandom.nextInt(revengeBosses.length)];
            default:
                String[] genericBosses = {"Uralt Drache", "Lich König", "Elementarfürst", "Götterfeind"};
                return genericBosses[seedRandom.nextInt(genericBosses.length)];
        }
    }

    private String generateStorylineReward(String theme, Random seedRandom) {
        String[] rewards = {"Legendäre Waffe", "Titel und Ehre", "Magische Macht", "Weltfrieden",
                          "Große Schätze", "Königliche Anerkennung", "Göttlicher Segen"};
        return rewards[seedRandom.nextInt(rewards.length)];
    }

    @Override
    public String getPhaseType() {
        return "QUEST_GENERATION";
    }

    @Override
    public String getProcessorName() {
        return "SimpleQuestProcessor";
    }
}
