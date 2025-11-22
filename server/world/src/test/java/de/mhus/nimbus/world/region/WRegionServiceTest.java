package de.mhus.nimbus.world.region;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class WRegionServiceTest {

    @Test
    void createWorldToken_privateKeyUsed() throws Exception {
        JwtService jwtService = Mockito.mock(JwtService.class);
        KeyService keyService = Mockito.mock(KeyService.class);
        WRegionProperties props = new WRegionProperties(); props.setBaseUrl("http://localhost:8080");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC"); kpg.initialize(256); KeyPair pair = kpg.generateKeyPair();
        Mockito.when(keyService.getLatestPrivateKey(KeyType.WORLD, "w1")).thenReturn(Optional.of(pair.getPrivate()));
        Mockito.when(jwtService.createTokenWithSecretKey(pair.getPrivate(), "w1", null, Mockito.any())).thenReturn("TOK");
        // Builder mit einfacher RestTemplate Instanz
        RestTemplateBuilder builder = new RestTemplateBuilder();
        WRegionService service = new WRegionService(builder, props, jwtService, keyService);
        // indirekt über deleteWorld Aufruf (nutzt authHeaders -> createWorldToken)
        try {
            service.deleteWorld("r1","w1");
        } catch (Exception ignored) { // kein echter Server erreichbar
        }
        Mockito.verify(jwtService).createTokenWithSecretKey(Mockito.eq(pair.getPrivate()), Mockito.eq("w1"), Mockito.isNull(), Mockito.any());
    }
}

