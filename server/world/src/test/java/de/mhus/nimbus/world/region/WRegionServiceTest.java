package de.mhus.nimbus.world.region;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyIntent;
import de.mhus.nimbus.shared.security.KeyService;
import de.mhus.nimbus.shared.security.KeyType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class WRegionServiceTest {

    @Test
    void createWorldToken_privateKeyUsed() throws Exception {
        JwtService jwtService = Mockito.mock(JwtService.class);
        KeyService keyService = Mockito.mock(KeyService.class);
        WRegionProperties props = new WRegionProperties(); props.setBaseUrl("http://localhost:8080");
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC"); kpg.initialize(256); KeyPair pair = kpg.generateKeyPair();
        KeyIntent intent = KeyIntent.of("w1", KeyIntent.MAIN_JWT_TOKEN);
        Mockito.when(keyService.getLatestPrivateKey(KeyType.WORLD, intent)).thenReturn(Optional.of(pair.getPrivate()));
        Mockito.when(jwtService.createTokenWithSecretKey(
                Mockito.eq(pair.getPrivate()),
                Mockito.eq("w1"),
                isNull(),
                any(Instant.class)
        )).thenReturn("TOK");
        RestTemplateBuilder builder = new RestTemplateBuilder();
        WRegionService service = new WRegionService(builder, props, jwtService, keyService);
        try {
            service.deleteWorld("r1","w1"); // triggert Token-Erstellung
        } catch (Exception ignored) { }
        Mockito.verify(jwtService).createTokenWithSecretKey(eq(pair.getPrivate()), eq("w1"), isNull(), any());
        ArgumentCaptor<KeyIntent> intentCaptor = ArgumentCaptor.forClass(KeyIntent.class);
        Mockito.verify(keyService).getLatestPrivateKey(eq(KeyType.WORLD), intentCaptor.capture());
        assertEquals("w1", intentCaptor.getValue().owner());
        assertEquals(KeyIntent.MAIN_JWT_TOKEN, intentCaptor.getValue().intent());
    }
}
