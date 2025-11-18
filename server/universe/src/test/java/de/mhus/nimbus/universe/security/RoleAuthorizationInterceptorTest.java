package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.universe.user.UUser;
import de.mhus.nimbus.shared.user.UniverseRoles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        TestHttpServletResponse resp = new TestHttpServletResponse();
        boolean result = interceptor.preHandle(req, resp, hm("admin"));
        assertThat(result).isFalse();
        assertThat(resp.status).isEqualTo(401);
    }

    @Test
    void adminEndpoint_denies_userRole() throws Exception {
        setUser(UniverseRoles.USER);
        HttpServletRequest req = mock(HttpServletRequest.class);
        TestHttpServletResponse resp = new TestHttpServletResponse();
        boolean result = interceptor.preHandle(req, resp, hm("admin"));
        assertThat(result).isFalse();
        assertThat(resp.status).isEqualTo(403);
    }

    @Test
    void adminEndpoint_allows_adminRole() throws Exception {
        setUser(UniverseRoles.ADMIN);
        HttpServletRequest req = mock(HttpServletRequest.class);
        TestHttpServletResponse resp = new TestHttpServletResponse();
        boolean result = interceptor.preHandle(req, resp, hm("admin"));
        assertThat(result).isTrue();
    }

    @Test
    void userEndpoint_allows_userRole() throws Exception {
        setUser(UniverseRoles.USER);
        HttpServletRequest req = mock(HttpServletRequest.class);
        TestHttpServletResponse resp = new TestHttpServletResponse();
        boolean result = interceptor.preHandle(req, resp, hm("user"));
        assertThat(result).isTrue();
    }

    @Test
    void userEndpoint_allows_adminRole() throws Exception {
        setUser(UniverseRoles.ADMIN);
        HttpServletRequest req = mock(HttpServletRequest.class);
        TestHttpServletResponse resp = new TestHttpServletResponse();
        boolean result = interceptor.preHandle(req, resp, hm("user"));
        assertThat(result).isTrue();
    }

    // Minimal stub response to capture status
    static class TestHttpServletResponse implements HttpServletResponse {
        int status = 200;
        @Override public void setStatus(int sc) { this.status = sc; }
        // --- Unimplemented methods (not needed for tests) ---
        @Override public void addCookie(jakarta.servlet.http.Cookie cookie) {}
        @Override public boolean containsHeader(String name) { return false; }
        @Override public String encodeURL(String url) { return null; }
        @Override public String encodeRedirectURL(String url) { return null; }
        @Override public String encodeUrl(String url) { return null; }
        @Override public String encodeRedirectUrl(String url) { return null; }
        @Override public void sendError(int sc, String msg) {}
        @Override public void sendError(int sc) {}
        @Override public void sendRedirect(String location) {}
        @Override public void setDateHeader(String name, long date) {}
        @Override public void addDateHeader(String name, long date) {}
        @Override public void setHeader(String name, String value) {}
        @Override public void addHeader(String name, String value) {}
        @Override public void setIntHeader(String name, int value) {}
        @Override public void addIntHeader(String name, int value) {}
        @Override public void setStatus(int sc, String sm) { this.status = sc; }
        @Override public int getStatus() { return status; }
        @Override public String getHeader(String name) { return null; }
        @Override public java.util.Collection<String> getHeaders(String name) { return java.util.List.of(); }
        @Override public java.util.Collection<String> getHeaderNames() { return java.util.List.of(); }
        @Override public String getCharacterEncoding() { return null; }
        @Override public String getContentType() { return null; }
        @Override public jakarta.servlet.ServletOutputStream getOutputStream() { return null; }
        @Override public java.io.PrintWriter getWriter() { return new java.io.PrintWriter(System.out); }
        @Override public void setCharacterEncoding(String charset) {}
        @Override public void setContentLength(int len) {}
        @Override public void setContentLengthLong(long len) {}
        @Override public void setContentType(String type) {}
        @Override public void setBufferSize(int size) {}
        @Override public int getBufferSize() { return 0; }
        @Override public void flushBuffer() {}
        @Override public void resetBuffer() {}
        @Override public boolean isCommitted() { return false; }
        @Override public void reset() {}
        @Override public void setLocale(java.util.Locale loc) {}
        @Override public java.util.Locale getLocale() { return java.util.Locale.getDefault(); }
    }
}

