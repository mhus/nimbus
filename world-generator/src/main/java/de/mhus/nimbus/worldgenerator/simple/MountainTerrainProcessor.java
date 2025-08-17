package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * Simple Terrain-Generierung für bergige Landschaften.
 * Erstellt Berge, Täler und alpine Landschaften.
 */
@Component
@Slf4j
public class MountainTerrainProcessor implements PhaseProcessor {

    private final Random random = new Random();

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Berg-Terrain-Generierung für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        Map<String, Object> parameters = phase.getParameters();
        int worldSize = (Integer) parameters.getOrDefault("worldSize", 1000);
        int seed = (Integer) parameters.getOrDefault("seed", random.nextInt(1000000));

        Random seedRandom = new Random(seed);

        // Simuliere Terrain-Generierung
        Thread.sleep(2500 + random.nextInt(3500));

        // Generiere Bergketten
        int mountainRanges = 2 + seedRandom.nextInt(4); // 2-5 Bergketten

        for (int i = 0; i < mountainRanges; i++) {
            generateMountainRange(i + 1, worldSize, seedRandom);
        }

        // Generiere Täler zwischen den Bergen
        generateValleys(worldSize, mountainRanges, seedRandom);

        // Generiere alpine Features
        generateAlpineFeatures(worldSize, seedRandom);

        log.info("Berg-Terrain generiert - {} Bergketten erstellt", mountainRanges);
    }

    private void generateMountainRange(int rangeId, int worldSize, Random seedRandom) {
        // Bergkette Parameter
        int startX = seedRandom.nextInt(worldSize);
        int startY = seedRandom.nextInt(worldSize);
        int length = 200 + seedRandom.nextInt(400);
        float direction = seedRandom.nextFloat() * 360; // Richtung in Grad

        float maxHeight = 800 + seedRandom.nextFloat() * 1200; // 800-2000m
        int peakCount = 3 + seedRandom.nextInt(8); // 3-10 Gipfel pro Kette

        log.info("Bergkette {} - Start: ({}, {}), Länge: {}m, Max-Höhe: {:.0f}m, Gipfel: {}",
                rangeId, startX, startY, length, maxHeight, peakCount);

        // Generiere individuelle Gipfel
        for (int i = 0; i < peakCount; i++) {
            float peakHeight = maxHeight * (0.6f + seedRandom.nextFloat() * 0.4f);
            int peakX = startX + (int)(Math.cos(Math.toRadians(direction)) * (length * i / peakCount));
            int peakY = startY + (int)(Math.sin(Math.toRadians(direction)) * (length * i / peakCount));

            generatePeak(rangeId, i + 1, peakX, peakY, peakHeight, seedRandom);
        }
    }

    private void generatePeak(int rangeId, int peakId, int x, int y, float height, Random seedRandom) {
        // Gipfel-Eigenschaften
        String[] peakTypes = {"spitzer_gipfel", "plateau", "doppelgipfel", "kamm"};
        String peakType = peakTypes[seedRandom.nextInt(peakTypes.length)];

        int baseRadius = 50 + seedRandom.nextInt(100); // Basis-Radius

        log.debug("Gipfel {}.{} - Position: ({}, {}), Höhe: {:.0f}m, Typ: {}, Radius: {}m",
                rangeId, peakId, x, y, height, peakType, baseRadius);

        // Generiere Hänge um den Gipfel
        generateSlopes(x, y, height, baseRadius, seedRandom);
    }

    private void generateSlopes(int centerX, int centerY, float peakHeight, int radius, Random seedRandom) {
        // Verschiedene Hangneigungen
        String[] slopeTypes = {"sanft", "steil", "klippe", "terrassiert"};

        for (int angle = 0; angle < 360; angle += 45) {
            String slopeType = slopeTypes[seedRandom.nextInt(slopeTypes.length)];
            float slopeGradient = getSlopeGradient(slopeType, seedRandom);

            log.debug("Hang bei {}° - Typ: {}, Neigung: {:.1f}°",
                    angle, slopeType, slopeGradient);
        }
    }

    private float getSlopeGradient(String slopeType, Random seedRandom) {
        switch (slopeType) {
            case "sanft": return 5 + seedRandom.nextFloat() * 15; // 5-20°
            case "steil": return 25 + seedRandom.nextFloat() * 25; // 25-50°
            case "klippe": return 60 + seedRandom.nextFloat() * 30; // 60-90°
            case "terrassiert": return 10 + seedRandom.nextFloat() * 20; // 10-30° mit Stufen
            default: return 15;
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
}
