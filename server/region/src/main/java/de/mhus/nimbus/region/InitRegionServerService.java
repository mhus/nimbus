package de.mhus.nimbus.region;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class InitRegionServerService {

    private static final Logger LOG = LoggerFactory.getLogger(InitRegionServerService.class);

    @Value("${region.server.id}")
    private String regionServerId;

    @Value("${universe.url}")
    private String universeUrl;

    // Pfade für Key Dateien (konfidential Ordner relativ zum Projekt)
    private final Path confidentialDir = Path.of("confidential");
    private final Path publicKeyFile = confidentialDir.resolve("regionServerPublicKey.txt");
    private final Path privateKeyFile = confidentialDir.resolve("regionServerPrivateKey.pkcs8");

    @PostConstruct
    public void init() {
        LOG.info("InitRegionServerService gestartet für ServerId='{}' (UniverseUrl={})", regionServerId, universeUrl);
        try {
            ensureKeyPair();
        } catch (Exception e) {
            LOG.error("Fehler beim Initialisieren des Region Server Keypairs: {}", e.toString(), e);
        }
    }

    private void ensureKeyPair() throws Exception {
        if (Files.exists(publicKeyFile) && Files.exists(privateKeyFile)) {
            LOG.info("RegionServer Keypair Dateien vorhanden - überspringe Erzeugung");
            return;
        }
        LOG.info("Kein vollständiges RegionServer Keypair gefunden - erzeugt neues RSA 2048 Paar");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        PublicKey pub = kp.getPublic();
        PrivateKey priv = kp.getPrivate();

        // Verzeichnisse sicherstellen
        if (!Files.exists(confidentialDir)) {
            Files.createDirectories(confidentialDir);
        }

        // Speichern: Public Key als PEM-like (BEGIN/END) oder einfach Base64
        String pubB64 = Base64.getEncoder().encodeToString(pub.getEncoded());
        Files.writeString(publicKeyFile, pubB64 + System.lineSeparator(), StandardCharsets.UTF_8);
        LOG.info("RegionServer Public Key gespeichert: {} ({} Bytes Base64)", publicKeyFile.toAbsolutePath(), pubB64.length());

        String privB64 = Base64.getEncoder().encodeToString(priv.getEncoded());
        Files.writeString(privateKeyFile, privB64 + System.lineSeparator(), StandardCharsets.UTF_8);
        LOG.info("RegionServer Private Key gespeichert: {} ({} Bytes Base64) - SCHÜTZE DIESE DATEI!", privateKeyFile.toAbsolutePath(), privB64.length());
    }

    // Hilfsmethoden (optional) um Schlüssel später zu laden
    public PublicKey loadPublicKey() throws Exception {
        if (!Files.exists(publicKeyFile)) throw new IOException("Public key file missing: " + publicKeyFile);
        String b64 = Files.readString(publicKeyFile, StandardCharsets.UTF_8).trim();
        byte[] der = Base64.getDecoder().decode(b64);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    public PrivateKey loadPrivateKey() throws Exception {
        if (!Files.exists(privateKeyFile)) throw new IOException("Private key file missing: " + privateKeyFile);
        String b64 = Files.readString(privateKeyFile, StandardCharsets.UTF_8).trim();
        byte[] der = Base64.getDecoder().decode(b64);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    public String getRegionServerId() { return regionServerId; }
    public String getUniverseUrl() { return universeUrl; }
}

