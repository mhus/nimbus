package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.universe.user.UserService;
import de.mhus.nimbus.universe.user.User;
import de.mhus.nimbus.shared.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserService userService;
    private final RequestUserHolder userHolder;

    private static final String AUTH_BASE = "/universe/user/auth";

    public JwtAuthenticationFilter(JwtService jwtService, JwtProperties jwtProperties, UserService userService, RequestUserHolder userHolder) {
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userService = userService;
        this.userHolder = userHolder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // Allow auth endpoints without token
        if (isPublicPath(path)) {
            chain.doFilter(request, response);
            return;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String token = auth.substring(7).trim();
        Optional<Jws<Claims>> claimsOpt = jwtService.validateTokenWithSecretKey(token, jwtProperties.getKeyId());
        if (claimsOpt.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        Claims claims = claimsOpt.get().getPayload();
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        User user = userService.getById(userId).orElse(null);
        CurrentUser cu = new CurrentUser(userId, username, user);
        userHolder.set(cu);
        try {
            request.setAttribute("currentUser", cu);
            chain.doFilter(request, response);
        } finally {
            userHolder.clear();
        }
    }

    private boolean isPublicPath(String path) {
        return (AUTH_BASE + "/login").equals(path) || (AUTH_BASE + "/logout").equals(path);
    }
}
