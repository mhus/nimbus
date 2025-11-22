package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.shared.security.JwtService;
import de.mhus.nimbus.shared.security.KeyType;
import de.mhus.nimbus.universe.user.UUserService;
import de.mhus.nimbus.universe.user.UUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UUserJwtAuthenticationFilterTest {

    private JwtService jwtService;
    private UUserService userService;
    private RequestUserHolder holder;
    private UUserJwtAuthenticationFilter filter;

    @BeforeEach
    void setup() {
        jwtService = Mockito.mock(JwtService.class);
        userService = Mockito.mock(UUserService.class);
        holder = new RequestUserHolder();
        filter = new UUserJwtAuthenticationFilter(jwtService, userService, holder);
    }

    @Test
    void unauthorizedWithoutHeader() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET","/other");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilterInternal(req, resp, (request, response) -> {});
        assertEquals(401, resp.getStatus());
    }

    @Test
    void authorizedWithValidToken() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET","/other");
        req.addHeader("Authorization", "Bearer token123");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        @SuppressWarnings("unchecked") Jws<Claims> jws = Mockito.mock(Jws.class);
        Claims claims = Mockito.mock(Claims.class);
        Mockito.when(jws.getPayload()).thenReturn(claims);
        Mockito.when(claims.getSubject()).thenReturn("u1");
        Mockito.when(claims.get("username", String.class)).thenReturn("user1");
        Mockito.when(claims.get("typ")).thenReturn("access");
        Mockito.when(jwtService.validateTokenWithPublicKey("token123", KeyType.UNIVERSE, "system")).thenReturn(Optional.of(jws));
        UUser user = new UUser(); user.setId("u1"); user.setUsername("user1");
        Mockito.when(userService.getById("u1")).thenReturn(Optional.of(user));
        java.util.concurrent.atomic.AtomicReference<CurrentUser> captured = new java.util.concurrent.atomic.AtomicReference<>();
        filter.doFilter(req, resp, (request, response) -> {
            assertNotNull(holder.get(), "Holder sollte innerhalb der Chain gesetzt sein");
            captured.set(holder.get());
        });
        assertEquals(200, resp.getStatus());
        assertNotNull(captured.get(), "CurrentUser wurde nicht erfasst");
        assertEquals("u1", captured.get().userId());
        // Nach Filterlauf ist Holder geleert, aber Attribut bleibt bestehen
        assertNull(holder.get(), "Holder sollte nach Filterlauf geleert sein");
        Object attr = req.getAttribute("currentUser");
        assertNotNull(attr, "Request Attribut currentUser sollte gesetzt sein");
        assertTrue(attr instanceof CurrentUser);
        assertEquals("u1", ((CurrentUser)attr).userId());
    }

    @Test
    void unauthorizedDisabledUser() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET","/other");
        req.addHeader("Authorization", "Bearer tokenABC");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        @SuppressWarnings("unchecked") Jws<Claims> jws = Mockito.mock(Jws.class);
        Claims claims = Mockito.mock(Claims.class);
        Mockito.when(jws.getPayload()).thenReturn(claims);
        Mockito.when(claims.getSubject()).thenReturn("u2");
        Mockito.when(claims.get("username", String.class)).thenReturn("user2");
        Mockito.when(claims.get("typ")).thenReturn("access");
        Mockito.when(jwtService.validateTokenWithPublicKey("tokenABC", KeyType.UNIVERSE, "system")).thenReturn(Optional.of(jws));
        UUser user = new UUser(); user.setId("u2"); user.setUsername("user2"); user.setEnabled(false);
        Mockito.when(userService.getById("u2")).thenReturn(Optional.of(user));
        filter.doFilter(req, resp, (r, s) -> fail("Chain sollte nicht erreicht werden"));
        assertEquals(401, resp.getStatus());
        assertNull(holder.get());
    }
}
