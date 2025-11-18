package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.shared.user.UniverseRoles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RoleAuthorizationInterceptorTest {

    private RequestUserHolder holder;
    private RoleAuthorizationInterceptor interceptor;
    private DummyController controller;

    static class DummyController {
        @Role(UniverseRoles.ADMIN)
        public void admin() {}
        @Role({UniverseRoles.USER, UniverseRoles.ADMIN})
        public void user() {}
        public void open() {}
    }

    @BeforeEach
    void setup() {
        holder = new RequestUserHolder();
        interceptor = new RoleAuthorizationInterceptor(holder);
        controller = new DummyController();
    }

    private HandlerMethod hm(String name) throws NoSuchMethodException {
        Method m = DummyController.class.getMethod(name);
        return new HandlerMethod(controller, m);
    }

    private void setUser(UniverseRoles... roles) {
        UUser u = new UUser("u","u@example.com");
        u.setId("id1");
        if (roles != null && roles.length>0) u.setRoles(roles); else u.setRoles();
        holder.set(new CurrentUser(u.getId(), u.getUsername(), u));
    }

    @Test
    void openEndpoint_noAnnotation_allowsWithoutUser() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        boolean result = interceptor.preHandle(req, resp, hm("open"));
        assertThat(result).isTrue();
    }

    @Test
    void adminEndpoint_denies_withoutUser() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(req, resp, hm("admin"));
        assertThat(result).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }

    @Test
    void adminEndpoint_denies_userRole() throws Exception {
        setUser(UniverseRoles.USER);
        HttpServletRequest req = mock(HttpServletRequest.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(req, resp, hm("admin"));
        assertThat(result).isFalse();
        assertThat(resp.getStatus()).isEqualTo(403);
    }

    @Test
    void adminEndpoint_allows_adminRole() throws Exception {
        setUser(UniverseRoles.ADMIN);
        HttpServletRequest req = mock(HttpServletRequest.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(req, resp, hm("admin"));
        assertThat(result).isTrue();
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    @Test
    void userEndpoint_allows_userRole() throws Exception {
        setUser(UniverseRoles.USER);
        HttpServletRequest req = mock(HttpServletRequest.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(req, resp, hm("user"));
        assertThat(result).isTrue();
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    @Test
    void userEndpoint_allows_adminRole() throws Exception {
        setUser(UniverseRoles.ADMIN);
        HttpServletRequest req = mock(HttpServletRequest.class);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        boolean result = interceptor.preHandle(req, resp, hm("user"));
        assertThat(result).isTrue();
        assertThat(resp.getStatus()).isEqualTo(200);
    }
}
