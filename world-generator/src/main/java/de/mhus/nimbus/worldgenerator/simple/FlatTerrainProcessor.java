package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.entity.WorldGenerator;
import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import de.mhus.nimbus.worldgenerator.repository.WorldGeneratorRepository;
import de.mhus.nimbus.world.shared.client.WorldTerrainClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Simple Terrain-Generierung für flache Landschaften.
 * Erstellt ebene Flächen mit minimaler Höhenvariation.
 */
@Component
@Slf4j
public class FlatTerrainProcessor implements PhaseProcessor {

    private final Random random = new Random();

    @Autowired
    private WorldTerrainClient worldTerrainClient;

    @Autowired
    private WorldGeneratorRepository worldGeneratorRepository;

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Flachland-Terrain-Generierung für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        // Lade WorldGenerator Entität und extrahiere WorldId
        WorldGenerator worldGenerator = worldGeneratorRepository.findById(phase.getWorldGeneratorId())
                .orElseThrow(() -> new RuntimeException("WorldGenerator nicht gefunden: " + phase.getWorldGeneratorId()));

        String worldId = worldGenerator.getParameters().get("worldId");
        if (worldId == null) {
            throw new RuntimeException("WorldId nicht in den Properties der WorldGenerator Entität gefunden");
        }

        log.info("Verwende WorldId: {} für Terrain-Generierung", worldId);

        Map<String, Object> parameters = phase.getParameters();
        int worldSize = Integer.parseInt(worldGenerator.getParameters().getOrDefault("worldSize", "1000"));
        int seed = Integer.parseInt(worldGenerator.getParameters().getOrDefault("seed", String.valueOf(random.nextInt(1000000))));

        Random seedRandom = new Random(seed);

        // Simuliere Terrain-Generierung
        Thread.sleep(1500 + random.nextInt(2500));

        // Generiere flaches Terrain mit minimaler Variation
        float baseHeight = 50.0f; // Basis-Höhe in Metern
        float maxVariation = 5.0f; // Maximale Höhenvariation

        int clusterSize = 64; // 64x64 Felder pro Cluster
        int clustersPerSide = worldSize / clusterSize;

        List<TerrainCluster> clusters = new ArrayList<>();

        for (int clusterX = 0; clusterX < clustersPerSide; clusterX++) {
            for (int clusterY = 0; clusterY < clustersPerSide; clusterY++) {
                TerrainCluster cluster = generateFlatCluster(clusterX, clusterY, clusterSize, baseHeight, maxVariation, seedRandom);
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

        // Generiere Flüsse und Bäche
        generateRivers(worldSize, seedRandom);

        log.info("Flachland-Terrain generiert und an World Terrain Service übertragen - Basis-Höhe: {:.1f}m, Variation: ±{:.1f}m",
                baseHeight, maxVariation);
    }

    private TerrainCluster generateFlatCluster(int clusterX, int clusterY, int clusterSize, float baseHeight, float maxVariation, Random seedRandom) {
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

                // Berechne Höhe mit minimaler Variation
                float height = baseHeight + (seedRandom.nextFloat() - 0.5f) * maxVariation;

                // Gelegentliche kleine Hügel für Abwechslung
                if (seedRandom.nextDouble() < 0.05) { // 5% Chance
                    height += 10 + seedRandom.nextFloat() * 15; // Kleine Hügel
                }

                // Gelegentliche flache Täler
                if (seedRandom.nextDouble() < 0.03) { // 3% Chance
                    height -= 5 + seedRandom.nextFloat() * 10; // Flache Senken
                }

                field.sizeZ = Math.max(1, (int) height);
                field.opacity = 255;

                // Setze Materialien basierend auf Höhe
                field.materials = generateFlatMaterials(height, seedRandom);
                field.groups = Arrays.asList(1, 2); // Terrain-Gruppen

                // Parameter für Terrain-Details
                field.parameters = new HashMap<>();
                field.parameters.put("height", String.valueOf(height));
                field.parameters.put("grassDensity", String.valueOf(70 + seedRandom.nextInt(30)));
                field.parameters.put("soilType", "loam");

                cluster.fields.add(field);
            }
        }

        return cluster;
    }

    private List<Integer> generateFlatMaterials(float height, Random seedRandom) {
        List<Integer> materials = new ArrayList<>();

        // Basis-Schichtung für flaches Terrain
        if (height < 45) {
            // Niedriges Gebiet - mehr Wasser und Schlamm
            materials.addAll(Arrays.asList(4, 2, 2, 2, 2, 3)); // Wasser, Erde, Gras
        } else if (height < 55) {
            // Normales flaches Land
            materials.addAll(Arrays.asList(1, 2, 2, 2, 2, 3)); // Gras dominiert
        } else {
            // Leicht erhöhtes Gebiet
            materials.addAll(Arrays.asList(1, 2, 5, 2, 2, 3)); // Mit etwas Sand
        }

        return materials;
    }

    private void sendTerrainBatch(String worldId, List<TerrainCluster> clusters) {
        try {
            TerrainRequest request = new TerrainRequest();
            request.world = worldId;
            request.clusters = clusters;

            worldTerrainClient.createTerrain(request);
            log.debug("Terrain-Batch mit {} Clustern erfolgreich übertragen", clusters.size());

        } catch (Exception e) {
            log.error("Fehler beim Übertragen des Terrain-Batches: {}", e.getMessage(), e);
            throw new RuntimeException("Fehler beim Übertragen des Terrains: " + e.getMessage(), e);
        }
    }

    private void generateRivers(int worldSize, Random seedRandom) {
        int riverCount = 2 + seedRandom.nextInt(4); // 2-5 Flüsse

        for (int i = 0; i < riverCount; i++) {
            int startX = seedRandom.nextInt(worldSize);
            int startY = seedRandom.nextInt(worldSize);
            int riverLength = 100 + seedRandom.nextInt(300);

            log.debug("Fluss {} - Start: ({}, {}), Länge: {}m", i+1, startX, startY, riverLength);
        }
    }

    @Override
    public String getPhaseType() {
        return "TERRAIN_GENERATION";
    }

    @Override
    public String getProcessorName() {
        return "FlatTerrainProcessor";
    }

    // DTOs für Terrain-Übertragung
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
