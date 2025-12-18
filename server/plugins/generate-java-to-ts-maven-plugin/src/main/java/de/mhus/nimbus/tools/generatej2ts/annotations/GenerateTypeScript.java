package de.mhus.nimbus.tools.generatej2ts.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Markiert eine Java-Klasse oder ein Enum zur Generierung eines TypeScript-Typs.
 *
 * Verwendung:
 * - @GenerateTypeScript("subfolder")
 * - @GenerateTypeScript(value = "subfolder")
 *
 * Der optionale Wert definiert einen Unterordner relativ zum Ausgabeordner,
 * in dem die erzeugte .ts-Datei abgelegt wird.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface GenerateTypeScript {
    /**
     * Optionaler Unterordner für die Ausgabe (z. B. "models").
     */
    String value() default "";
}
