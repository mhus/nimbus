package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import de.mhus.nimbus.worldgenerator.repository.WorldGeneratorRepository;
import de.mhus.nimbus.worldshared.client.WorldTerrainClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Simple Terrain-Generierung für bergige Landschaften.
 * Erstellt Berge, Täler und alpine Landschaften.
 */
@Component
@Slf4j
public class MountainTerrainProcessor implements PhaseProcessor {

    private final Random random = new Random();

    @Autowired
    private WorldTerrainClient worldTerrainClient;

    @Autowired
    private WorldGeneratorRepository worldGeneratorRepository;

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Berg-Terrain-Generierung für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        // Lade WorldGenerator Entität und extrahiere WorldId
        WorldGenerator worldGenerator = worldGeneratorRepository.findById(phase.getWorldGeneratorId())
                .orElseThrow(() -> new RuntimeException("WorldGenerator nicht gefunden: " + phase.getWorldGeneratorId()));

        String worldId = worldGenerator.getParameters().get("worldId");
        if (worldId == null) {
            throw new RuntimeException("WorldId nicht in den Properties der WorldGenerator Entität gefunden");
        }

        log.info("Verwende WorldId: {} für Berg-Terrain-Generierung", worldId);

        Map<String, Object> parameters = phase.getParameters();
        int worldSize = Integer.parseInt(worldGenerator.getParameters().getOrDefault("worldSize", "1000"));
        int seed = Integer.parseInt(worldGenerator.getParameters().getOrDefault("seed", String.valueOf(random.nextInt(1000000))));

        Random seedRandom = new Random(seed);

        // Simuliere Terrain-Generierung
        Thread.sleep(2500 + random.nextInt(3500));

        // Generiere Bergketten
        int mountainRanges = 2 + seedRandom.nextInt(4); // 2-5 Bergketten
        List<MountainRange> ranges = new ArrayList<>();

        for (int i = 0; i < mountainRanges; i++) {
            MountainRange range = generateMountainRange(i + 1, worldSize, seedRandom);
            ranges.add(range);
        }

        // Generiere Terrain-Cluster basierend auf den Bergketten
        int clusterSize = 64; // 64x64 Felder pro Cluster
        int clustersPerSide = worldSize / clusterSize;

        List<TerrainCluster> clusters = new ArrayList<>();

        for (int clusterX = 0; clusterX < clustersPerSide; clusterX++) {
            for (int clusterY = 0; clusterY < clustersPerSide; clusterY++) {
                TerrainCluster cluster = generateMountainCluster(clusterX, clusterY, clusterSize, ranges, seedRandom);
                clusters.add(cluster);

                if (clusters.size() >= 10) { // Batch-Verarbeitung
                    sendTerrainBatch(worldId, clusters);
                    clusters.clear();
                }
            }
        }

        // Sende verbleibende Cluster
        if (!clusters.isEmpty()) {
            sendTerrainBatch(worldId, clusters);
        }

        // Generiere Täler zwischen den Bergen
        generateValleys(worldSize, mountainRanges, seedRandom);

        // Generiere alpine Features
        generateAlpineFeatures(worldSize, seedRandom);

        log.info("Berg-Terrain generiert und an World Terrain Service übertragen - {} Bergketten erstellt", mountainRanges);
    }

    private MountainRange generateMountainRange(int rangeId, int worldSize, Random seedRandom) {
        // Bergkette Parameter
        int startX = seedRandom.nextInt(worldSize);
        int startY = seedRandom.nextInt(worldSize);
        int length = 200 + seedRandom.nextInt(400);
        float direction = seedRandom.nextFloat() * 360; // Richtung in Grad

        float maxHeight = 800 + seedRandom.nextFloat() * 1200; // 800-2000m
        int peakCount = 3 + seedRandom.nextInt(8); // 3-10 Gipfel pro Kette

        log.info("Bergkette {} - Start: ({}, {}), Länge: {}m, Max-Höhe: {:.0f}m, Gipfel: {}",
                rangeId, startX, startY, length, maxHeight, peakCount);

        MountainRange range = new MountainRange();
        range.id = rangeId;
        range.startX = startX;
        range.startY = startY;
        range.length = length;
        range.direction = direction;
        range.maxHeight = maxHeight;
        range.peaks = new ArrayList<>();

        // Generiere individuelle Gipfel
        for (int i = 0; i < peakCount; i++) {
            float peakHeight = maxHeight * (0.6f + seedRandom.nextFloat() * 0.4f);
            int peakX = startX + (int)(Math.cos(Math.toRadians(direction)) * (length * (float)i / peakCount));
            int peakY = startY + (int)(Math.sin(Math.toRadians(direction)) * (length * (float)i / peakCount));

            Peak peak = generatePeak(rangeId, i + 1, peakX, peakY, peakHeight, seedRandom);
            range.peaks.add(peak);
        }

        return range;
    }

    private Peak generatePeak(int rangeId, int peakId, int x, int y, float height, Random seedRandom) {
        // Gipfel-Eigenschaften
        String[] peakTypes = {"spitzer_gipfel", "plateau", "doppelgipfel", "kamm"};
        String peakType = peakTypes[seedRandom.nextInt(peakTypes.length)];

        int baseRadius = 50 + seedRandom.nextInt(100); // Basis-Radius

        log.debug("Gipfel {}.{} - Position: ({}, {}), Höhe: {:.0f}m, Typ: {}, Radius: {}m",
                rangeId, peakId, x, y, height, peakType, baseRadius);

        Peak peak = new Peak();
        peak.id = peakId;
        peak.x = x;
        peak.y = y;
        peak.height = height;
        peak.type = peakType;
        peak.radius = baseRadius;

        return peak;
    }

    private TerrainCluster generateMountainCluster(int clusterX, int clusterY, int clusterSize, List<MountainRange> ranges, Random seedRandom) {
        TerrainCluster cluster = new TerrainCluster();
        cluster.level = 0;
        cluster.x = clusterX;
        cluster.y = clusterY;
        cluster.fields = new ArrayList<>();

        for (int x = 0; x < clusterSize; x++) {
            for (int y = 0; y < clusterSize; y++) {
                TerrainField field = new TerrainField();
                field.x = clusterX * clusterSize + x;
                field.y = clusterY * clusterSize + y;
                field.z = 0;

                // Berechne Höhe basierend auf Nähe zu Bergen
                float height = calculateHeightForPosition(field.x, field.y, ranges, seedRandom);

                field.sizeZ = Math.max(1, (int) height);
                field.opacity = 255;

                // Setze Materialien basierend auf Höhe
                field.materials = generateMountainMaterials(height, seedRandom);
                field.groups = Arrays.asList(1, 2, 3); // Terrain-Gruppen inkl. Berg-Gruppe

                // Parameter für Terrain-Details
                field.parameters = new HashMap<>();
                field.parameters.put("height", String.valueOf(height));
                field.parameters.put("slope", String.valueOf(calculateSlope(field.x, field.y, ranges)));
                field.parameters.put("rockType", getRockType(height, seedRandom));

                cluster.fields.add(field);
            }
        }

        return cluster;
    }

    private float calculateHeightForPosition(int x, int y, List<MountainRange> ranges, Random seedRandom) {
        float baseHeight = 100.0f; // Basis-Höhe für Bergregionen
        float maxInfluence = 0.0f;

        // Finde den nächsten Berg und berechne Einfluss
        for (MountainRange range : ranges) {
            for (Peak peak : range.peaks) {
                double distance = Math.sqrt(Math.pow(x - peak.x, 2) + Math.pow(y - peak.y, 2));
                if (distance < peak.radius * 2) {
                    float influence = (float) (1.0 - (distance / (peak.radius * 2)));
                    float heightContribution = peak.height * influence;
                    maxInfluence = Math.max(maxInfluence, heightContribution);
                }
            }
        }

        // Füge etwas zufällige Variation hinzu
        float variation = (seedRandom.nextFloat() - 0.5f) * 20;

        return baseHeight + maxInfluence + variation;
    }

    private List<Integer> generateMountainMaterials(float height, Random seedRandom) {
        List<Integer> materials = new ArrayList<>();

        // Material-Schichtung basierend auf Höhe
        if (height < 200) {
            // Niedrige Berghänge - Wald und Erde
            materials.addAll(Arrays.asList(1, 2, 6, 2, 2, 3)); // Gras, Erde, Stein
        } else if (height < 800) {
            // Mittlere Höhen - mehr Stein
            materials.addAll(Arrays.asList(6, 6, 2, 2, 7, 3)); // Stein dominiert
        } else if (height < 1500) {
            // Hohe Berge - hauptsächlich Stein
            materials.addAll(Arrays.asList(6, 6, 6, 7, 7, 8)); // Verschiedene Steinarten
        } else {
            // Gipfelregion - Schnee und Eis
            materials.addAll(Arrays.asList(9, 9, 6, 6, 7, 8)); // Schnee, Eis, Stein
        }

        return materials;
    }

    private float calculateSlope(int x, int y, List<MountainRange> ranges) {
        // Vereinfachte Neigungsberechnung
        float nearestPeakDistance = Float.MAX_VALUE;

        for (MountainRange range : ranges) {
            for (Peak peak : range.peaks) {
                float distance = (float) Math.sqrt(Math.pow(x - peak.x, 2) + Math.pow(y - peak.y, 2));
                nearestPeakDistance = Math.min(nearestPeakDistance, distance);
            }
        }

        // Je näher zum Gipfel, desto steiler
        return Math.min(60.0f, 1000.0f / Math.max(nearestPeakDistance, 10.0f));
    }

    private String getRockType(float height, Random seedRandom) {
        String[] lowAltitudeRocks = {"sandstein", "kalkstein", "schiefer"};
        String[] highAltitudeRocks = {"granit", "basalt", "gneis"};

        if (height < 500) {
            return lowAltitudeRocks[seedRandom.nextInt(lowAltitudeRocks.length)];
        } else {
            return highAltitudeRocks[seedRandom.nextInt(highAltitudeRocks.length)];
        }
    }

    private void sendTerrainBatch(String worldId, List<TerrainCluster> clusters) {
        try {
            TerrainRequest request = new TerrainRequest();
            request.world = worldId;
            request.clusters = clusters;

            worldTerrainClient.createTerrain(request);
            log.debug("Berg-Terrain-Batch mit {} Clustern erfolgreich übertragen", clusters.size());

        } catch (Exception e) {
            log.error("Fehler beim Übertragen des Berg-Terrain-Batches: {}", e.getMessage(), e);
            throw new RuntimeException("Fehler beim Übertragen des Berg-Terrains: " + e.getMessage(), e);
        }
    }

    private void generateValleys(int worldSize, int mountainRanges, Random seedRandom) {
        int valleyCount = mountainRanges + seedRandom.nextInt(3); // Mindestens so viele wie Bergketten

        for (int i = 0; i < valleyCount; i++) {
            int valleyX = seedRandom.nextInt(worldSize);
            int valleyY = seedRandom.nextInt(worldSize);
            int valleyLength = 100 + seedRandom.nextInt(300);
            int valleyWidth = 20 + seedRandom.nextInt(50);

            String[] valleyTypes = {"u_tal", "v_tal", "schlucht", "hochtal"};
            String valleyType = valleyTypes[seedRandom.nextInt(valleyTypes.length)];

            log.debug("Tal {} - Position: ({}, {}), Länge: {}m, Breite: {}m, Typ: {}",
                    i+1, valleyX, valleyY, valleyLength, valleyWidth, valleyType);
        }
    }

    private void generateAlpineFeatures(int worldSize, Random seedRandom) {
        // Gletscher
        int glacierCount = seedRandom.nextInt(4); // 0-3 Gletscher
        for (int i = 0; i < glacierCount; i++) {
            int glacierX = seedRandom.nextInt(worldSize);
            int glacierY = seedRandom.nextInt(worldSize);
            int glacierSize = 50 + seedRandom.nextInt(150);

            log.debug("Gletscher {} - Position: ({}, {}), Größe: {}m",
                    i+1, glacierX, glacierY, glacierSize);
        }

        // Wasserfälle
        int waterfallCount = 3 + seedRandom.nextInt(8); // 3-10 Wasserfälle
        for (int i = 0; i < waterfallCount; i++) {
            int waterfallX = seedRandom.nextInt(worldSize);
            int waterfallY = seedRandom.nextInt(worldSize);
            float waterfallHeight = 20 + seedRandom.nextFloat() * 180; // 20-200m

            log.debug("Wasserfall {} - Position: ({}, {}), Höhe: {:.0f}m",
                    i+1, waterfallX, waterfallY, waterfallHeight);
        }

        // Bergseen
        int lakeCount = 2 + seedRandom.nextInt(6); // 2-7 Bergseen
        for (int i = 0; i < lakeCount; i++) {
            int lakeX = seedRandom.nextInt(worldSize);
            int lakeY = seedRandom.nextInt(worldSize);
            int lakeRadius = 10 + seedRandom.nextInt(40);

            log.debug("Bergsee {} - Position: ({}, {}), Radius: {}m",
                    i+1, lakeX, lakeY, lakeRadius);
        }
    }

    @Override
    public String getPhaseType() {
        return "TERRAIN_GENERATION";
    }

    @Override
    public String getProcessorName() {
        return "MountainTerrainProcessor";
    }

    // DTOs für Berg-Terrain
    public static class MountainRange {
        public int id;
        public int startX;
        public int startY;
        public int length;
        public float direction;
        public float maxHeight;
        public List<Peak> peaks;
    }

    public static class Peak {
        public int id;
        public int x;
        public int y;
        public float height;
        public String type;
        public int radius;
    }

    // Terrain-DTOs (gemeinsam mit FlatTerrainProcessor)
    public static class TerrainRequest {
        public String world;
        public List<TerrainCluster> clusters;
    }

    public static class TerrainCluster {
        public int level;
        public int x;
        public int y;
        public List<TerrainField> fields;
    }

    public static class TerrainField {
        public int x;
        public int y;
        public int z;
        public List<Integer> groups;
        public List<Integer> materials;
        public int opacity;
        public int sizeZ;
        public Map<String, String> parameters;
    }
}
