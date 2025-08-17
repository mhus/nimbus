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
 * Simple Historische Generierung für Weltgeschichte und Hintergrundinformationen.
 * Erstellt Zeitalter, Ereignisse und kulturelle Entwicklungen.
 */
@Component
@Slf4j
public class SimpleHistoryProcessor implements PhaseProcessor {

    private final Random random = new Random();

    private final List<String> HISTORICAL_ERAS = Arrays.asList(
        "Urzeit", "Steinzeit", "Bronzezeit", "Eisenzeit", "Antike",
        "Mittelalter", "Renaissance", "Neuzeit", "Moderne"
    );

    private final List<String> CIVILIZATIONS = Arrays.asList(
        "Waldelfen", "Bergzwerge", "Wüstennomaden", "Seefahrer",
        "Drachenvölker", "Kristallmagier", "Steinriesen", "Windläufer"
    );

    private final List<String> EVENT_TYPES = Arrays.asList(
        "Krieg", "Naturkatastrophe", "Entdeckung", "Kultureller_Wandel",
        "Magisches_Ereignis", "Handel", "Migration", "Erfindung"
    );

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Historische Generierung für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        Map<String, Object> parameters = phase.getParameters();
        int seed = (Integer) parameters.getOrDefault("seed", random.nextInt(1000000));
        int historyDepth = (Integer) parameters.getOrDefault("historyDepth", 5); // Anzahl Zeitalter

        Random seedRandom = new Random(seed);

        // Simuliere Generierungszeit
        Thread.sleep(1000 + random.nextInt(2000));

        // Generiere historische Zeitalter
        for (int era = 0; era < Math.min(historyDepth, HISTORICAL_ERAS.size()); era++) {
            generateHistoricalEra(era, HISTORICAL_ERAS.get(era), seedRandom);
        }

        // Generiere übergreifende historische Verbindungen
        generateHistoricalConnections(historyDepth, seedRandom);

        log.info("Historische Generierung abgeschlossen - {} Zeitalter erstellt", historyDepth);
    }

    private void generateHistoricalEra(int eraIndex, String eraName, Random seedRandom) {
        int eraLength = 100 + seedRandom.nextInt(500); // 100-600 Jahre
        int eventCount = 2 + seedRandom.nextInt(6); // 2-7 Ereignisse pro Zeitalter

        log.info("Zeitalter '{}' (Index: {}) - Dauer: {} Jahre, Ereignisse: {}",
                eraName, eraIndex, eraLength, eventCount);

        // Dominante Zivilisation in diesem Zeitalter
        String dominantCiv = CIVILIZATIONS.get(seedRandom.nextInt(CIVILIZATIONS.size()));
        log.debug("Dominante Zivilisation in {}: {}", eraName, dominantCiv);

        // Generiere Ereignisse für dieses Zeitalter
        for (int i = 0; i < eventCount; i++) {
            generateHistoricalEvent(eraName, i + 1, eraLength, dominantCiv, seedRandom);
        }

        // Kulturelle Entwicklungen
        generateCulturalDevelopments(eraName, dominantCiv, seedRandom);
    }

    private void generateHistoricalEvent(String era, int eventId, int eraLength,
                                       String dominantCiv, Random seedRandom) {
        String eventType = EVENT_TYPES.get(seedRandom.nextInt(EVENT_TYPES.size()));
        int eventYear = seedRandom.nextInt(eraLength);
        int eventDuration = getEventDuration(eventType, seedRandom);

        String eventDescription = generateEventDescription(eventType, dominantCiv, seedRandom);

        log.debug("Ereignis {}.{} ({}) - Jahr: {}, Dauer: {} Jahre - {}",
                era, eventId, eventType, eventYear, eventDuration, eventDescription);

        // Auswirkungen des Ereignisses
        generateEventConsequences(eventType, dominantCiv, seedRandom);
    }

    private String generateEventDescription(String eventType, String civilization, Random seedRandom) {
        switch (eventType) {
            case "Krieg":
                String enemy = CIVILIZATIONS.get(seedRandom.nextInt(CIVILIZATIONS.size()));
                return civilization + " führt Krieg gegen " + enemy;
            case "Naturkatastrophe":
                String[] disasters = {"Erdbeben", "Vulkanausbruch", "Große Flut", "Dürre", "Meteoriteneinschlag"};
                return disasters[seedRandom.nextInt(disasters.length)] + " erschüttert das Land von " + civilization;
            case "Entdeckung":
                String[] discoveries = {"neue Länder", "magische Artefakte", "seltene Erze", "antike Ruinen", "neue Handelsrouten"};
                return civilization + " entdeckt " + discoveries[seedRandom.nextInt(discoveries.length)];
            case "Kultureller_Wandel":
                String[] changes = {"neue Religion", "Kunstrichtung", "Philosophie", "Gesellschaftsform", "Sprache"};
                return "Aufkommen einer neuen " + changes[seedRandom.nextInt(changes.length)] + " bei " + civilization;
            case "Magisches_Ereignis":
                String[] magic = {"Große Erweckung", "Magie-Sturm", "Portale öffnen sich", "Drachen erwachen", "Sterne fallen"};
                return magic[seedRandom.nextInt(magic.length)] + " - " + civilization + " ist betroffen";
            case "Handel":
                String partner = CIVILIZATIONS.get(seedRandom.nextInt(CIVILIZATIONS.size()));
                return civilization + " beginnt Handel mit " + partner;
            case "Migration":
                return "Große Wanderung von " + civilization + " in neue Gebiete";
            case "Erfindung":
                String[] inventions = {"Schrift", "Metallverarbeitung", "Architektur", "Navigation", "Landwirtschaft"};
                return civilization + " entwickelt " + inventions[seedRandom.nextInt(inventions.length)];
            default:
                return "Unbekanntes Ereignis bei " + civilization;
        }
    }

    private int getEventDuration(String eventType, Random seedRandom) {
        switch (eventType) {
            case "Krieg": return 1 + seedRandom.nextInt(20); // 1-20 Jahre
            case "Naturkatastrophe": return seedRandom.nextInt(3); // 0-2 Jahre
            case "Entdeckung": return seedRandom.nextInt(5); // 0-4 Jahre
            case "Kultureller_Wandel": return 10 + seedRandom.nextInt(40); // 10-50 Jahre
            case "Magisches_Ereignis": return seedRandom.nextInt(10); // 0-9 Jahre
            case "Handel": return 5 + seedRandom.nextInt(95); // 5-100 Jahre
            case "Migration": return 2 + seedRandom.nextInt(18); // 2-20 Jahre
            case "Erfindung": return 1 + seedRandom.nextInt(9); // 1-10 Jahre
            default: return 1;
        }
    }

    private void generateEventConsequences(String eventType, String civilization, Random seedRandom) {
        int positiveImpact = seedRandom.nextInt(100) - 50; // -50 bis +49
        int economicImpact = seedRandom.nextInt(100) - 50;
        int culturalImpact = seedRandom.nextInt(100) - 50;

        log.debug("Auswirkungen für {} - Positiv: {}, Wirtschaft: {}, Kultur: {}",
                civilization, positiveImpact, economicImpact, culturalImpact);
    }

    private void generateCulturalDevelopments(String era, String civilization, Random seedRandom) {
        String[] developments = {
            "Kunstwerke", "Architekturstil", "Handwerkstechniken",
            "Musikrichtung", "Literatur", "Wissenschaft"
        };

        int devCount = 1 + seedRandom.nextInt(4); // 1-4 Entwicklungen

        for (int i = 0; i < devCount; i++) {
            String development = developments[seedRandom.nextInt(developments.length)];
            log.debug("Kulturelle Entwicklung in {} ({}): {}", era, civilization, development);
        }
    }

    private void generateHistoricalConnections(int eraCount, Random seedRandom) {
        // Generiere Verbindungen zwischen Zeitaltern
        for (int i = 1; i < eraCount; i++) {
            String connection = generateEraConnection(HISTORICAL_ERAS.get(i-1), HISTORICAL_ERAS.get(i), seedRandom);
            log.debug("Übergang {} → {}: {}", HISTORICAL_ERAS.get(i-1), HISTORICAL_ERAS.get(i), connection);
        }

        // Generiere Legenden und Mythen
        int legendCount = 2 + seedRandom.nextInt(5); // 2-6 Legenden
        for (int i = 0; i < legendCount; i++) {
            generateLegend(i + 1, seedRandom);
        }
    }

    private String generateEraConnection(String fromEra, String toEra, Random seedRandom) {
        String[] connectionTypes = {
            "Technologischer Fortschritt", "Politischer Wandel", "Kulturelle Revolution",
            "Naturereignis", "Krieg", "Entdeckung", "Migration"
        };

        return connectionTypes[seedRandom.nextInt(connectionTypes.length)];
    }

    private void generateLegend(int legendId, Random seedRandom) {
        String[] legendTypes = {
            "Heldensage", "Schöpfungsmythos", "Dämonenjagd", "Verlorene Stadt",
            "Magisches Artefakt", "Prophezeihung", "Geisterschlacht", "Drachentöter"
        };

        String legendType = legendTypes[seedRandom.nextInt(legendTypes.length)];
        String civilization = CIVILIZATIONS.get(seedRandom.nextInt(CIVILIZATIONS.size()));

        log.debug("Legende {} - Typ: '{}', Ursprung: {}", legendId, legendType, civilization);
    }

    @Override
    public String getPhaseType() {
        return "HISTORY_GENERATION";
    }

    @Override
    public String getProcessorName() {
        return "SimpleHistoryProcessor";
    }
}
