package de.mhus.nimbus.tools.generatej2ts.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation zur Steuerung der TypeScript-Generierung auf Feldebene.
 *
 * Unterstützte Attribute:
 * - follow: Wenn true, soll dem Feldtyp gefolgt und dieser ebenfalls generiert werden (falls markiert)
 * - type: Überschreibt den abgeleiteten TypeScript-Typ (z. B. "number" oder "Foo[]")
 * - import: Fügt eine Import-Zeile in die generierte TS-Datei ein (z. B. "import { Foo } from './Foo';")
 * - ignore: Wenn true, wird das Feld im generierten TS-Typ ignoriert
 * - optional: Wenn true, wird das Feld als optional ausgegeben (z. B. "bar?: string;")
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD})
public @interface TypeScript {
    boolean follow() default false;
    String type() default "";
    // Da "import" ein Java-Schlüsselwort ist, kann das Element nicht exakt so heißen.
    // Der Parser unterstützt mehrere Alias-Namen (tsImport, import_, importPath, importValue, importAs),
    // um dennoch Import-Zeilen zu erfassen.
    String tsImport() default "";    // bevorzugter Alias
    String importAs() default "";    // weitere Aliase
    String import_() default "";
    String importValue() default "";
    String importPath() default "";
    boolean ignore() default false;
    boolean optional() default false;
}
