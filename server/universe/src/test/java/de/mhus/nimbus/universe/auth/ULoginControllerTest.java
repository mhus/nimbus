package de.mhus.nimbus.universe.auth;

import de.mhus.nimbus.universe.api.ULoginController;
import de.mhus.nimbus.universe.security.USecurityService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class ULoginControllerTest {

    @Test
    void login_success() {
        USecurityService sec = Mockito.mock(USecurityService.class);
        ULoginController controller = new ULoginController(sec);
        ULoginResponse respPayload = new ULoginResponse("accessTok","refreshTok","u1","user1");
        Mockito.when(sec.login(Mockito.any())).thenReturn(USecurityService.AuthResult.ok(respPayload));
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("user1","pw"));
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("accessTok", resp.getBody().token());
        assertEquals("refreshTok", resp.getBody().refreshToken());
    }

    @Test
    void refresh_success() {
        USecurityService sec = Mockito.mock(USecurityService.class);
        ULoginController controller = new ULoginController(sec);
        ULoginResponse respPayload = new ULoginResponse("newAccess","newRefresh","u1","user1");
        Mockito.when(sec.refresh("Bearer oldRefresh")).thenReturn(USecurityService.AuthResult.ok(respPayload));
        ResponseEntity<ULoginResponse> resp = controller.refresh("Bearer oldRefresh");
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("newAccess", resp.getBody().token());
        assertEquals("newRefresh", resp.getBody().refreshToken());
    }

    @Test
    void login_badRequest() {
        USecurityService sec = Mockito.mock(USecurityService.class);
        ULoginController controller = new ULoginController(sec);
        Mockito.when(sec.login(Mockito.any())).thenReturn(USecurityService.AuthResult.of(org.springframework.http.HttpStatus.BAD_REQUEST));
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("user", null));
        assertEquals(400, resp.getStatusCodeValue());
        assertNull(resp.getBody());
    }

    @Test
    void login_unauthorized() {
        USecurityService sec = Mockito.mock(USecurityService.class);
        ULoginController controller = new ULoginController(sec);
        Mockito.when(sec.login(Mockito.any())).thenReturn(USecurityService.AuthResult.of(org.springframework.http.HttpStatus.UNAUTHORIZED));
        ResponseEntity<ULoginResponse> resp = controller.login(new ULoginRequest("user1", "wrong"));
        assertEquals(401, resp.getStatusCodeValue());
    }

    @Test
    void refresh_missingHeader() {
        USecurityService sec = Mockito.mock(USecurityService.class);
        ULoginController controller = new ULoginController(sec);
        Mockito.when(sec.refresh(null)).thenReturn(USecurityService.AuthResult.of(org.springframework.http.HttpStatus.UNAUTHORIZED));
        ResponseEntity<ULoginResponse> resp = controller.refresh(null);
        assertEquals(401, resp.getStatusCodeValue());
    }

    @Test
    void refresh_invalidToken() {
        USecurityService sec = Mockito.mock(USecurityService.class);
        ULoginController controller = new ULoginController(sec);
        Mockito.when(sec.refresh("Bearer old")).thenReturn(USecurityService.AuthResult.of(org.springframework.http.HttpStatus.UNAUTHORIZED));
        ResponseEntity<ULoginResponse> resp = controller.refresh("Bearer old");
        assertEquals(401, resp.getStatusCodeValue());
    }
}
