package de.mhus.nimbus.universe.favorit;

import de.mhus.nimbus.universe.security.CurrentUser;
import de.mhus.nimbus.universe.security.RequestUserHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FavoritControllerTest {

    private FavoritService service;
    private RequestUserHolder userHolder;
    private FavoritController controller;

    @BeforeEach
    void setup() {
        service = mock(FavoritService.class);
        userHolder = mock(RequestUserHolder.class);
        controller = new FavoritController(service, userHolder);
        when(userHolder.get()).thenReturn(new CurrentUser("u1","alpha", null));
    }

    @Test
    void listFavorites_empty() {
        when(service.listFavorites("u1")).thenReturn(List.of());
        var resp = controller.listFavorites();
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    void create_ok() {
        Favorit f = new Favorit("u1","q1","s1","w1","e1","Title", true);
        f.setId("fav1");
        f.setCreatedAt(Instant.now());
        f.setLastAccessAt(Instant.now());
        when(service.create(eq("u1"), eq("q1"), eq("s1"), eq("w1"), eq("e1"), eq("Title"), eq(true))).thenReturn(f);
        var req = new FavoritController.FavoritRequest("q1","s1","w1","e1","Title", true);
        var resp = controller.create(req);
        assertEquals(201, resp.getStatusCode().value());
        assertEquals("fav1", resp.getBody().id());
    }

    @Test
    void get_notFound() {
        when(service.getById("x")).thenReturn(Optional.empty());
        var resp = controller.get("x");
        assertEquals(404, resp.getStatusCode().value());
    }

    @Test
    void update_ok() {
        Favorit existing = new Favorit("u1","q1","s1","w1","e1","Old", false);
        existing.setId("fav1");
        existing.setCreatedAt(Instant.now());
        existing.setLastAccessAt(Instant.now());
        when(service.getById("fav1")).thenReturn(Optional.of(existing));
        Favorit updated = new Favorit("u1","q2","s2","w2","e2","New", true);
        updated.setId("fav1");
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setLastAccessAt(Instant.now());
        when(service.update("fav1","q2","s2","w2","e2","New", true)).thenReturn(updated);
        var req = new FavoritController.FavoritRequest("q2","s2","w2","e2","New", true);
        var resp = controller.update("fav1", req);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("q2", resp.getBody().quadrantId());
        assertTrue(resp.getBody().favorit());
    }

    @Test
    void toggle_ok() {
        Favorit existing = new Favorit("u1","q1","s1","w1","e1","Old", false);
        existing.setId("fav1");
        existing.setCreatedAt(Instant.now());
        existing.setLastAccessAt(Instant.now());
        when(service.getById("fav1")).thenReturn(Optional.of(existing));
        Favorit toggled = new Favorit("u1","q1","s1","w1","e1","Old", true);
        toggled.setId("fav1");
        toggled.setCreatedAt(existing.getCreatedAt());
        toggled.setLastAccessAt(Instant.now());
        when(service.toggleFavorite("fav1", true)).thenReturn(toggled);
        var resp = controller.toggle("fav1", true);
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().favorit());
        verify(service).toggleFavorite("fav1", true);
    }

    @Test
    void delete_ok() {
        Favorit existing = new Favorit("u1","q1","s1","w1","e1","Old", false);
        existing.setId("fav1");
        when(service.getById("fav1")).thenReturn(Optional.of(existing));
        var resp = controller.delete("fav1");
        assertEquals(204, resp.getStatusCode().value());
        verify(service).delete("fav1");
    }
}

