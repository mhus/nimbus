package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Simple Asset Processor für die Generierung und Verwaltung der Assets.
 * Lädt Assets aus den resources und speichert Materialien in der Datenbank.
 */
@Component("simpleAssetProcessor")
@Slf4j
public class SimpleAssetProcessor implements PhaseProcessor {

    private static final List<String> BASIC_ASSETS = Arrays.asList(
        "gras", "sand", "wasser", "felsen", "baum", "blume"
    );

    private static final List<String> GROUND_ASSETS = Arrays.asList(
        "gras_boden", "sand_boden", "wasser_boden", "felsen_boden", "baum_boden", "blume_boden"
    );

    private static final List<String> SPECIAL_ASSETS = Arrays.asList(
        "pfad", "stein", "wasserfall", "fluss", "schnee", "lava", "eis", "moos", "pilz", "kristall"
    );

    private static final List<String> WATER_ASSETS = Arrays.asList(
        "koralle", "muschel", "algen"
    );

    private static final List<String> SWAMP_ASSETS = Arrays.asList(
        "schilf", "gras_sumpf", "sand_sumpf", "wasser_sumpf", "felsen_sumpf",
        "baum_sumpf", "blume_sumpf", "pfad_sumpf", "stein_sumpf",
        "wasserfall_sumpf", "fluss_sumpf", "schnee_sumpf", "lava_sumpf"
    );

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Processing simple asset generation phase: {}", phase.getName());

        // Assets laden und verarbeiten
        loadBasicAssets();
        loadGroundAssets();
        loadSpecialAssets();
        loadWaterAssets();
        loadSwampAssets();

        // Materialien erstellen
        generateMaterials(phase);

        log.info("Simple asset generation completed for phase: {}", phase.getName());
    }

    private void loadBasicAssets() throws InterruptedException {
        log.info("Loading basic assets: {}", BASIC_ASSETS);
        Thread.sleep(200); // Simuliert Asset-Ladezeit
    }

    private void loadGroundAssets() throws InterruptedException {
        log.info("Loading ground assets: {}", GROUND_ASSETS);
        Thread.sleep(200); // Simuliert Asset-Ladezeit
    }

    private void loadSpecialAssets() throws InterruptedException {
        log.info("Loading special assets: {}", SPECIAL_ASSETS);
        Thread.sleep(200); // Simuliert Asset-Ladezeit
    }

    private void loadWaterAssets() throws InterruptedException {
        log.info("Loading water assets: {}", WATER_ASSETS);
        Thread.sleep(200); // Simuliert Asset-Ladezeit
    }

    private void loadSwampAssets() throws InterruptedException {
        log.info("Loading swamp assets: {}", SWAMP_ASSETS);
        Thread.sleep(200); // Simuliert Asset-Ladezeit
    }

    private void generateMaterials(PhaseInfo phase) throws InterruptedException {
        log.info("Generating materials for all loaded assets");
        Thread.sleep(500); // Simuliert Material-Generierung
    }

    @Override
    public String getPhaseType() {
        return "SIMPLE_ASSET";
    }

    @Override
    public String getProcessorName() {
        return "simpleAssetProcessor";
    }
}
