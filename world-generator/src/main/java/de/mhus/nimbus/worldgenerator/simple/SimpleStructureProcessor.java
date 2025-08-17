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
 * Simple Struktur-Generierung für Gebäude, Dörfer und Konstruktionen.
 * Erstellt Siedlungen, Bauwerke und architektonische Strukturen.
 */
@Component
@Slf4j
public class SimpleStructureProcessor implements PhaseProcessor {

    private final Random random = new Random();

    private final List<String> SETTLEMENT_TYPES = Arrays.asList(
        "Dorf", "Stadt", "Festung", "Kloster", "Handelsposten",
        "Bergwerk", "Hafen", "Turm", "Ruine", "Tempel"
    );

    private final List<String> BUILDING_TYPES = Arrays.asList(
        "Wohnhaus", "Werkstatt", "Lager", "Stall", "Brunnen",
        "Marktplatz", "Gasthof", "Schmiede", "Mühle", "Brücke"
    );

    private final List<String> ARCHITECTURAL_STYLES = Arrays.asList(
        "Holzbauweise", "Steinbauweise", "Fachwerk", "Lehmziegel",
        "Kristallarchitektur", "Baumhausarchitektur", "Höhlenbauweise", "Nomadenzelte"
    );

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Struktur-Generierung für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        Map<String, Object> parameters = phase.getParameters();
        int worldSize = (Integer) parameters.getOrDefault("worldSize", 1000);
        int seed = (Integer) parameters.getOrDefault("seed", random.nextInt(1000000));
        int structureDensity = (Integer) parameters.getOrDefault("structureDensity", 3); // 1-5 Dichte

        Random seedRandom = new Random(seed);

        // Simuliere Generierungszeit
        Thread.sleep(2000 + random.nextInt(3000));

        // Generiere Hauptsiedlungen
        int settlementCount = 3 + seedRandom.nextInt(8); // 3-10 Siedlungen
        for (int i = 0; i < settlementCount; i++) {
            generateSettlement(i + 1, worldSize, structureDensity, seedRandom);
        }

        // Generiere isolierte Strukturen
        generateIsolatedStructures(worldSize, structureDensity, seedRandom);

        // Generiere Verbindungswege
        generateRoadsAndPaths(settlementCount, worldSize, seedRandom);

        log.info("Struktur-Generierung abgeschlossen - {} Siedlungen und Strukturen erstellt", settlementCount);
    }

    private void generateSettlement(int settlementId, int worldSize, int density, Random seedRandom) {
        int x = seedRandom.nextInt(worldSize);
        int y = seedRandom.nextInt(worldSize);
        String settlementType = SETTLEMENT_TYPES.get(seedRandom.nextInt(SETTLEMENT_TYPES.size()));
        String architecturalStyle = ARCHITECTURAL_STYLES.get(seedRandom.nextInt(ARCHITECTURAL_STYLES.size()));

        int population = getSettlementPopulation(settlementType, seedRandom);
        int buildingCount = population / 3 + seedRandom.nextInt(population / 2 + 1);

        log.info("Siedlung {} '{}' - Position: ({}, {}), Stil: {}, Einwohner: {}, Gebäude: {}",
                settlementId, settlementType, x, y, architecturalStyle, population, buildingCount);

        // Generiere Zentrum der Siedlung
        generateSettlementCenter(settlementId, settlementType, architecturalStyle, seedRandom);

        // Generiere Gebäude
        for (int i = 0; i < buildingCount; i++) {
            generateBuilding(settlementId, i + 1, settlementType, architecturalStyle, seedRandom);
        }

        // Generiere Siedlungs-spezifische Strukturen
        generateSettlementSpecifics(settlementId, settlementType, seedRandom);
    }

    private int getSettlementPopulation(String settlementType, Random seedRandom) {
        switch (settlementType) {
            case "Dorf": return 50 + seedRandom.nextInt(200); // 50-250
            case "Stadt": return 500 + seedRandom.nextInt(2000); // 500-2500
            case "Festung": return 100 + seedRandom.nextInt(300); // 100-400
            case "Kloster": return 20 + seedRandom.nextInt(80); // 20-100
            case "Handelsposten": return 30 + seedRandom.nextInt(120); // 30-150
            case "Bergwerk": return 80 + seedRandom.nextInt(220); // 80-300
            case "Hafen": return 200 + seedRandom.nextInt(800); // 200-1000
            case "Turm": return 5 + seedRandom.nextInt(15); // 5-20
            case "Ruine": return 0; // Unbewohnt
            case "Tempel": return 10 + seedRandom.nextInt(40); // 10-50
            default: return 100;
        }
    }

    private void generateSettlementCenter(int settlementId, String type, String style, Random seedRandom) {
        String centerStructure = getCenterStructure(type, seedRandom);
        log.debug("Siedlung {} Zentrum: {} ({})", settlementId, centerStructure, style);
    }

    private String getCenterStructure(String settlementType, Random seedRandom) {
        switch (settlementType) {
            case "Dorf":
                String[] villageCenter = {"Dorfplatz", "Brunnen", "Gemeindehaus", "Kapelle"};
                return villageCenter[seedRandom.nextInt(villageCenter.length)];
            case "Stadt":
                String[] cityCenter = {"Marktplatz", "Rathaus", "Kathedrale", "Stadttor"};
                return cityCenter[seedRandom.nextInt(cityCenter.length)];
            case "Festung":
                String[] fortCenter = {"Hauptturm", "Kaserne", "Waffenkammer", "Kommandozentrale"};
                return fortCenter[seedRandom.nextInt(fortCenter.length)];
            case "Kloster":
                return "Hauptkirche";
            case "Handelsposten":
                return "Handelshalle";
            case "Bergwerk":
                return "Schachteingang";
            case "Hafen":
                return "Hafenmeisterei";
            case "Turm":
                return "Wachturm";
            case "Tempel":
                return "Haupttempel";
            default:
                return "Zentralgebäude";
        }
    }

    private void generateBuilding(int settlementId, int buildingId, String settlementType,
                                String style, Random seedRandom) {
        String buildingType = getBuildingTypeForSettlement(settlementType, seedRandom);
        int width = 5 + seedRandom.nextInt(15); // 5-20 Meter
        int length = 5 + seedRandom.nextInt(15);
        int height = 3 + seedRandom.nextInt(12); // 3-15 Meter

        log.debug("Gebäude {}.{} - Typ: {}, Größe: {}x{}x{}m, Stil: {}",
                settlementId, buildingId, buildingType, width, length, height, style);

        // Generiere Gebäude-Details
        generateBuildingDetails(buildingType, style, seedRandom);
    }

    private String getBuildingTypeForSettlement(String settlementType, Random seedRandom) {
        // Filtere Gebäudetypen basierend auf Siedlungstyp
        switch (settlementType) {
            case "Dorf":
                String[] villageBuildings = {"Wohnhaus", "Stall", "Brunnen", "Schmiede"};
                return villageBuildings[seedRandom.nextInt(villageBuildings.length)];
            case "Stadt":
                return BUILDING_TYPES.get(seedRandom.nextInt(BUILDING_TYPES.size()));
            case "Festung":
                String[] fortBuildings = {"Kaserne", "Waffenkammer", "Wachturm", "Stall"};
                return fortBuildings[seedRandom.nextInt(fortBuildings.length)];
            case "Bergwerk":
                String[] mineBuildings = {"Wohnhaus", "Lager", "Werkstatt", "Schmiede"};
                return mineBuildings[seedRandom.nextInt(mineBuildings.length)];
            default:
                return BUILDING_TYPES.get(seedRandom.nextInt(BUILDING_TYPES.size()));
        }
    }

    private void generateBuildingDetails(String buildingType, String style, Random seedRandom) {
        // Generiere Material-Details basierend auf Stil
        String primaryMaterial = getPrimaryMaterial(style, seedRandom);
        String roofType = getRoofType(style, seedRandom);

        log.debug("Details - Material: {}, Dach: {}", primaryMaterial, roofType);
    }

    private String getPrimaryMaterial(String style, Random seedRandom) {
        switch (style) {
            case "Holzbauweise": return "Holz";
            case "Steinbauweise": return "Stein";
            case "Fachwerk": return "Holz_und_Lehm";
            case "Lehmziegel": return "Lehm";
            case "Kristallarchitektur": return "Kristall";
            case "Baumhausarchitektur": return "Lebendiges_Holz";
            case "Höhlenbauweise": return "Fels";
            case "Nomadenzelte": return "Stoff_und_Leder";
            default: return "Mischbauweise";
        }
    }

    private String getRoofType(String style, Random seedRandom) {
        String[] roofTypes = {"Flachdach", "Satteldach", "Walmdach", "Spitzdach", "Kuppeldach"};
        return roofTypes[seedRandom.nextInt(roofTypes.length)];
    }

    private void generateSettlementSpecifics(int settlementId, String type, Random seedRandom) {
        switch (type) {
            case "Hafen":
                generatePortStructures(settlementId, seedRandom);
                break;
            case "Bergwerk":
                generateMineStructures(settlementId, seedRandom);
                break;
            case "Festung":
                generateFortificationStructures(settlementId, seedRandom);
                break;
            case "Tempel":
                generateTempleStructures(settlementId, seedRandom);
                break;
        }
    }

    private void generatePortStructures(int settlementId, Random seedRandom) {
        int dockCount = 2 + seedRandom.nextInt(6); // 2-7 Anlegestellen
        log.debug("Hafen {} - Anlegestellen: {}, Leuchtturm: {}",
                settlementId, dockCount, seedRandom.nextBoolean());
    }

    private void generateMineStructures(int settlementId, Random seedRandom) {
        int shaftCount = 1 + seedRandom.nextInt(4); // 1-4 Schächte
        String oreType = getOreType(seedRandom);
        log.debug("Bergwerk {} - Schächte: {}, Erz: {}", settlementId, shaftCount, oreType);
    }

    private String getOreType(Random seedRandom) {
        String[] ores = {"Eisen", "Gold", "Silber", "Kupfer", "Kohle", "Edelsteine", "Kristalle"};
        return ores[seedRandom.nextInt(ores.length)];
    }

    private void generateFortificationStructures(int settlementId, Random seedRandom) {
        int wallSections = 4 + seedRandom.nextInt(8); // 4-11 Mauerabschnitte
        int towerCount = 2 + seedRandom.nextInt(6); // 2-7 Türme
        log.debug("Festung {} - Mauerabschnitte: {}, Türme: {}", settlementId, wallSections, towerCount);
    }

    private void generateTempleStructures(int settlementId, Random seedRandom) {
        String deity = getDeity(seedRandom);
        int shrineCount = 1 + seedRandom.nextInt(5); // 1-5 Schreine
        log.debug("Tempel {} - Gottheit: {}, Schreine: {}", settlementId, deity, shrineCount);
    }

    private String getDeity(Random seedRandom) {
        String[] deities = {"Naturgott", "Kriegsgott", "Weisheitsgott", "Handelsgott",
                          "Feuergott", "Wassergott", "Erdgott", "Luftgott"};
        return deities[seedRandom.nextInt(deities.length)];
    }

    private void generateIsolatedStructures(int worldSize, int density, Random seedRandom) {
        int structureCount = density * 2 + seedRandom.nextInt(density * 3); // Basiert auf Dichte

        for (int i = 0; i < structureCount; i++) {
            generateIsolatedStructure(i + 1, worldSize, seedRandom);
        }
    }

    private void generateIsolatedStructure(int structureId, int worldSize, Random seedRandom) {
        String[] isolatedTypes = {"Einsiedlerhütte", "Wachturm", "Alte_Ruine", "Steinkreis",
                                "Wegkreuz", "Brücke", "Friedhof", "Schrein", "Leuchtfeuer"};

        String structureType = isolatedTypes[seedRandom.nextInt(isolatedTypes.length)];
        int x = seedRandom.nextInt(worldSize);
        int y = seedRandom.nextInt(worldSize);

        log.debug("Isolierte Struktur {} - Typ: '{}', Position: ({}, {})",
                structureId, structureType, x, y);
    }

    private void generateRoadsAndPaths(int settlementCount, int worldSize, Random seedRandom) {
        // Hauptstraßen zwischen Siedlungen
        int roadCount = Math.max(1, settlementCount / 2);

        for (int i = 0; i < roadCount; i++) {
            int startX = seedRandom.nextInt(worldSize);
            int startY = seedRandom.nextInt(worldSize);
            int endX = seedRandom.nextInt(worldSize);
            int endY = seedRandom.nextInt(worldSize);

            String roadType = getRoadType(seedRandom);
            int roadLength = (int) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));

            log.debug("Straße {} - Typ: {}, Von: ({}, {}) Nach: ({}, {}), Länge: {}m",
                    i+1, roadType, startX, startY, endX, endY, roadLength);
        }

        // Nebenwege und Pfade
        int pathCount = settlementCount + seedRandom.nextInt(settlementCount * 2);
        log.debug("Zusätzliche Pfade generiert: {}", pathCount);
    }

    private String getRoadType(Random seedRandom) {
        String[] roadTypes = {"Hauptstraße", "Nebenstraße", "Pfad", "Handelsroute", "Bergpfad"};
        return roadTypes[seedRandom.nextInt(roadTypes.length)];
    }

    @Override
    public String getPhaseType() {
        return "STRUCTURE_GENERATION";
    }

    @Override
    public String getProcessorName() {
        return "SimpleStructureProcessor";
    }
}
