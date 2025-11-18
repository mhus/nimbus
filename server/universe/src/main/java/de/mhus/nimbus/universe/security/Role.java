package de.mhus.nimbus.universe.security;

import de.mhus.nimbus.shared.user.UniverseRoles;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Role {
    UniverseRoles[] value();
}

