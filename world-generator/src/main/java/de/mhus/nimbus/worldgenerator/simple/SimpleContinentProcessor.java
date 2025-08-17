package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;

/**
 * Simple Kontinent-Generierung für Landmassen, Ozeane und geografische Merkmale.
 * Unterstützt Wald, Wüste, Ozean und Berge.
 */
@Component
@Slf4j
public class SimpleContinentProcessor implements PhaseProcessor {

    private final Random random = new Random();

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Kontinent-Generierung für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        Map<String, Object> parameters = phase.getParameters();
        int worldSize = (Integer) parameters.getOrDefault("worldSize", 1000);
        int seed = (Integer) parameters.getOrDefault("seed", random.nextInt(1000000));

        Random seedRandom = new Random(seed);

        // Simuliere Generierungszeit
        Thread.sleep(2000 + random.nextInt(3000));

        // Generiere Kontinente basierend auf dem Seed
        int numberOfContinents = 2 + seedRandom.nextInt(4); // 2-5 Kontinente

        for (int i = 0; i < numberOfContinents; i++) {
            generateContinent(i + 1, worldSize, seedRandom);
        }

        // Generiere Ozeane zwischen den Kontinenten
        generateOceans(worldSize, numberOfContinents, seedRandom);

        log.info("Kontinent-Generierung abgeschlossen - {} Kontinente erstellt", numberOfContinents);
    }

    private void generateContinent(int continentId, int worldSize, Random seedRandom) {
        // Zufällige Position und Größe für Kontinent
        int centerX = seedRandom.nextInt(worldSize);
        int centerY = seedRandom.nextInt(worldSize);
        int radius = 100 + seedRandom.nextInt(200);

        // Zufälliger dominanter Biom-Typ für diesen Kontinent
        String[] biomes = {"forest", "desert", "mountain", "mixed"};
        String dominantBiome = biomes[seedRandom.nextInt(biomes.length)];

        log.info("Kontinent {} generiert - Zentrum: ({}, {}), Radius: {}, Biom: {}",
                continentId, centerX, centerY, radius, dominantBiome);

        // Generiere Unterregionen im Kontinent
        generateContinentRegions(continentId, centerX, centerY, radius, dominantBiome, seedRandom);
    }

    private void generateContinentRegions(int continentId, int centerX, int centerY,
                                        int radius, String dominantBiome, Random seedRandom) {
        int regions = 3 + seedRandom.nextInt(5); // 3-7 Regionen pro Kontinent

        for (int i = 0; i < regions; i++) {
            // Regionale Variationen
            int regionX = centerX + (seedRandom.nextInt(radius) - radius/2);
            int regionY = centerY + (seedRandom.nextInt(radius) - radius/2);
            int regionSize = 20 + seedRandom.nextInt(50);

            String regionType = getRegionType(dominantBiome, seedRandom);

            log.debug("Region {}.{} - Position: ({}, {}), Größe: {}, Typ: {}",
                    continentId, i+1, regionX, regionY, regionSize, regionType);
        }
    }

    private String getRegionType(String dominantBiome, Random seedRandom) {
        switch (dominantBiome) {
            case "forest":
                String[] forestTypes = {"denseForest", "lightForest", "meadow", "riverbank"};
                return forestTypes[seedRandom.nextInt(forestTypes.length)];
            case "desert":
                String[] desertTypes = {"sandDunes", "rockDesert", "oasis", "canyon"};
                return desertTypes[seedRandom.nextInt(desertTypes.length)];
            case "mountain":
                String[] mountainTypes = {"highPeaks", "foothills", "valley", "plateau"};
                return mountainTypes[seedRandom.nextInt(mountainTypes.length)];
            case "mixed":
                String[] mixedTypes = {"forestEdge", "desertBorder", "coastalPlain", "hillside"};
                return mixedTypes[seedRandom.nextInt(mixedTypes.length)];
            default:
                return "plains";
        }
    }

    private void generateOceans(int worldSize, int numberOfContinents, Random seedRandom) {
        // Generiere Ozeane basierend auf verfügbarem Raum
        int oceanRegions = 2 + seedRandom.nextInt(3); // 2-4 Ozeanregionen

        for (int i = 0; i < oceanRegions; i++) {
            int oceanX = seedRandom.nextInt(worldSize);
            int oceanY = seedRandom.nextInt(worldSize);
            int oceanRadius = 150 + seedRandom.nextInt(250);

            String[] oceanTypes = {"deepOcean", "shallowSea", "archipelago", "strait"};
            String oceanType = oceanTypes[seedRandom.nextInt(oceanTypes.length)];

            log.info("Ozean {} generiert - Zentrum: ({}, {}), Radius: {}, Typ: {}",
                    i+1, oceanX, oceanY, oceanRadius, oceanType);
        }
    }

    @Override
    public String getPhaseType() {
        return "CONTINENT_GENERATION";
    }

    @Override
    public String getProcessorName() {
        return "SimpleContinentProcessor";
    }
}
