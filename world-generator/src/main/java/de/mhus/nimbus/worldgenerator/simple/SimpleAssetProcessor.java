package de.mhus.nimbus.worldgenerator.simple;

import de.mhus.nimbus.worldgenerator.model.PhaseInfo;
import de.mhus.nimbus.worldgenerator.processor.PhaseProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Simple Asset/Material-Generierung für Texturen und Materialien.
 * Erstellt PNG-Texturen für verschiedene Materialien.
 */
@Component
@Slf4j
public class SimpleAssetProcessor implements PhaseProcessor {

    private final Random random = new Random();

    // Asset-Namen aus der Spezifikation
    private final List<String> ASSETS = Arrays.asList(
        "gras", "sand", "wasser", "felsen", "baum", "blume",
        "gras_boden", "sand_boden", "wasser_boden", "felsen_boden",
        "baum_boden", "blume_boden", "pfad", "stein", "wasserfall",
        "fluss", "schnee", "lava", "eis", "moos", "pilz", "kristall",
        "koralle", "muschel", "algen", "schilf"
    );

    @Override
    public void processPhase(PhaseInfo phase) throws Exception {
        log.info("Starte Asset-Generierung für Welt-Generator ID: {}", phase.getWorldGeneratorId());

        // Simuliere Asset-Erstellung
        for (String asset : ASSETS) {
            generateAssetTexture(asset, phase.getWorldGeneratorId());
            Thread.sleep(50 + random.nextInt(100)); // Simuliere Generierungszeit
        }

        log.info("Asset-Generierung abgeschlossen - {} Assets erstellt", ASSETS.size());
    }

    private void generateAssetTexture(String assetName, Long worldId) throws IOException {
        // Erstelle 64x64 PNG Textur
        BufferedImage texture = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = texture.createGraphics();

        // Basis-Farbe basierend auf Asset-Typ
        Color baseColor = getBaseColorForAsset(assetName);
        g2d.setColor(baseColor);
        g2d.fillRect(0, 0, 64, 64);

        // Füge Rauschen hinzu für realistisches Aussehen
        addNoisePattern(g2d, baseColor);

        g2d.dispose();

        // Konvertiere zu PNG Bytes (in echter Implementierung würde dies in DB gespeichert)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(texture, "PNG", baos);
        byte[] textureData = baos.toByteArray();

        log.debug("Textur '{}' generiert: {} bytes", assetName, textureData.length);
    }

    private Color getBaseColorForAsset(String assetName) {
        switch (assetName) {
            case "gras": case "gras_boden": return new Color(34, 139, 34);
            case "sand": case "sand_boden": return new Color(194, 178, 128);
            case "wasser": case "wasser_boden": return new Color(65, 105, 225);
            case "felsen": case "felsen_boden": case "stein": return new Color(105, 105, 105);
            case "baum": case "baum_boden": return new Color(139, 69, 19);
            case "blume": case "blume_boden": return new Color(255, 20, 147);
            case "pfad": return new Color(160, 82, 45);
            case "wasserfall": case "fluss": return new Color(176, 224, 230);
            case "schnee": return new Color(255, 250, 250);
            case "lava": return new Color(255, 69, 0);
            case "eis": return new Color(173, 216, 230);
            case "moos": return new Color(124, 252, 0);
            case "pilz": return new Color(139, 69, 19);
            case "kristall": return new Color(230, 230, 250);
            case "koralle": return new Color(255, 127, 80);
            case "muschel": return new Color(255, 228, 196);
            case "algen": return new Color(46, 125, 50);
            case "schilf": return new Color(107, 142, 35);
            default: return new Color(128, 128, 128);
        }
    }

    private void addNoisePattern(Graphics2D g2d, Color baseColor) {
        // Füge subtiles Rauschen hinzu
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 64; y++) {
                if (random.nextDouble() < 0.1) {
                    int variation = random.nextInt(40) - 20;
                    Color noiseColor = new Color(
                        Math.max(0, Math.min(255, baseColor.getRed() + variation)),
                        Math.max(0, Math.min(255, baseColor.getGreen() + variation)),
                        Math.max(0, Math.min(255, baseColor.getBlue() + variation))
                    );
                    g2d.setColor(noiseColor);
                    g2d.fillRect(x, y, 1, 1);
                }
            }
        }
    }

    @Override
    public String getPhaseType() {
        return "ASSET_GENERATION";
    }

    @Override
    public String getProcessorName() {
        return "SimpleAssetProcessor";
    }
}
