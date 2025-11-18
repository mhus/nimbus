package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.shared.user.UniverseRoles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    private final RequestUserHolder userHolder;

    public RoleAuthorizationInterceptor(RequestUserHolder userHolder) {
        this.userHolder = userHolder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) return true; // not a controller method

        Role methodAnno = hm.getMethodAnnotation(Role.class);
        Role typeAnno = hm.getBeanType().getAnnotation(Role.class);

        UniverseRoles[] required = methodAnno != null ? methodAnno.value() : (typeAnno != null ? typeAnno.value() : null);
        if (required == null || required.length == 0) return true; // no restriction

        CurrentUser cu = userHolder.get();
        if (cu == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        Set<String> userRoles = cu.user() != null ? cu.user().getRoles().stream().map(Enum::name).collect(Collectors.toSet()) : Set.of();
        if (userRoles.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        // Allow if intersection not empty
        boolean allowed = Arrays.stream(required).map(Enum::name).anyMatch(userRoles::contains);
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }
}

