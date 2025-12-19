# generate-java-to-ts-maven-plugin

Maven-Plugin zur Generierung von TypeScript‑Typen (Interfaces/Enums) aus Java‑Klassen und ‑Enums.

Fokus: einfache, deklarative Nutzung über Annotationen in Java und optionale Konfiguration via `java-to-ts.yaml`.

## Features (aktueller Stand)

- Annotationen in Java steuern die Generierung:
  - `@GenerateTypeScript(value="subfolder")` markiert Klasse/Enum zur Generierung
    - Wenn `value` mit `.ts` endet, wird dies als Dateiname verwendet (z. B. `models/Custom.ts`)
    - Optional: `name="Human"` überschreibt den TypeScript‑Typnamen (Dateiname bleibt entweder aus `value` oder `<name>.ts`)
  - `@TypeScript(...)` für Felder: `follow`, `type`, `ignore`, `optional`, `description`, sowie Import‑Aliasfelder `tsImport`/`import_`/`importPath`/`importValue`/`importAs`
  - `@TypeScriptImport({"import ...", ...})` auf Klassen/Enums für zusätzliche TS‑Importzeilen
- Unterstützt Klassen (als `export interface`) und Enums (als `export enum`)
- Nur Felder werden berücksichtigt (Methoden werden ignoriert)
- Follows: Bei `@TypeScript(follow=true)` werden referenzierte Typen (heuristisch) mit eingesammelt, wenn sie ebenfalls annotiert sind
- Generics und Collections werden einfach abgebildet: `List<T>`, `Set<T>` → `T[]`, `Optional<T>` → `T`, `Map<K,V>` → `Record<string, V>`
- Standard‑Typmapping: `Instant` und `java.time.Instant` → `Date` (kann via Config überschrieben werden)
- Pro TS‑Datei ein Header‑Kommentar inkl. Java‑Source‑FQN: `Source: <package.Klasse>`
- Feld‑Beschreibung: `@TypeScript(description="...")` wird als Kommentar `/* ... */` an das TS‑Feld gehängt
- Default‑Imports aus Config mit Pfadtiefen‑Anpassung für relative Pfade

## Installation/Build

Das Plugin ist Teil dieses Multi‑Modul‑Projekts. Bauen Sie das Modul (und Abhängigkeiten) mit:

```bash
mvn -q -pl plugins/generate-java-to-ts-maven-plugin -am clean package -DskipTests
```

## Verwendung im Projekt (pom.xml)

Das Plugin kann in einem Zielmodul (z. B. `world-shared`) eingebunden werden:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>de.mhus.nimbus</groupId>
      <artifactId>generate-java-to-ts-maven-plugin</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <executions>
        <execution>
          <id>java-to-ts</id>
          <phase>generate-resources</phase>
          <goals>
            <goal>generate</goal>
          </goals>
          <configuration>
            <inputDirectory>${project.basedir}/src/main/java</inputDirectory>
            <outputDirectory>${project.build.directory}/ts-generated</outputDirectory>
            <!-- optional: Konfiguration -->
            <configFile>${project.basedir}/java-to-ts.yaml</configFile>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
  </build>
```

### Mojo‑Parameter

- `inputDirectory` (Default: `${project.basedir}/src/main/java`)
- `outputDirectory` (Default: `${project.basedir}/src/main/ts-generated`)
- `configFile` (optional; Default: `${project.basedir}/java-to-ts.yaml`)

## Annotationen

### GenerateTypeScript

```java
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface GenerateTypeScript {
    String value() default ""; // Unterordner oder (wenn mit .ts endet) Dateiname inkl. Pfad
    String name() default "";  // Optionaler Interface/Enum‑Name in TypeScript
}
```

Beispiele:

- `@GenerateTypeScript("models")` → Datei `models/<JavaName>.ts`
- `@GenerateTypeScript(value="models", name="Human")` → Datei `models/Human.ts`, `export interface Human { ... }`
- `@GenerateTypeScript("models/named/Custom.ts")` → Datei exakt `models/named/Custom.ts`, Interface‑Name weiterhin Java‑Typ (oder `name`, falls gesetzt)

### TypeScript (Feld‑Annotation)

Signatur (vereinfacht):

- `follow` (boolean): Folgetypen einsammeln
- `type` (String): TS‑Typ überschreiben, z. B. `"number"`, `"Foo[]"`
- `ignore` (boolean): Feld ignorieren
- `optional` (boolean): Optionales Feld → `?:` in TS
- `description` (String): Kommentar am Feld `/* ... */`
- Import‑Aliase: `tsImport`, `import_`, `importPath`, `importValue`, `importAs` (da `import` in Java reserviert ist)

### TypeScriptImport (Klassen/Enum‑Annotation)

```java
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface TypeScriptImport {
    String[] value();
}
```

Fügt Zeilen wie `import { X } from '...';` an den Anfang der TS‑Datei an.

## Konfiguration: java-to-ts.yaml

Optionale YAML‑Datei zur Feinsteuerung.

### 1) typeMappings (Java → TypeScript)

Ermöglicht individuelle Typ‑Mappings. Zusätzlich ist ein Default aktiv: `Instant` → `Date` (sowohl `Instant` als auch `java.time.Instant`).

Unterstützte Formate:

- Map

```yaml
typeMappings:
  java.time.Instant: Date
  java.util.UUID: string
  MyMoney: string
```

- Liste von Objekten

```yaml
typeMappings:
  - { java: java.time.Instant, ts: Date }
  - { java: java.util.UUID, ts: string }
```

Hinweise:

- FQN und SimpleName werden geprüft (z. B. `java.time.Instant` und `Instant`).
- Projektweite Defaults lassen sich so leicht anpassen/ergänzen.

### 2) defaultImports (für Interface‑Dateien)

Zusätzliche Import‑Zeilen, die in jede generierte Interface‑Datei eingefügt werden (Enums bleiben unberührt). Akzeptiert String oder Liste.

Beispiel:

```yaml
defaultImports:
  - "import { Util } from 'utils/Util';"
  - "import something from '@scope/some';"
```

Pfadregel für relative Pfade: Wenn der `from`‑Pfad weder mit `@`, `.` noch `/` beginnt, wird er abhängig von der Ordner‑Tiefe (aus `@GenerateTypeScript(value="...")`) automatisch mit `../` vorangestellt (ein `../` je Ebene). Aliase mit `@` oder bereits relative/absolute Pfade bleiben unverändert.

## Generierungsregeln (Kurzüberblick)

- Klassen → `export interface <Name> { ... }`
- Enums → `export enum <Name> { ... }`
- Optionales Feld → `foo?: string;`
- Beschreibung → `foo: string; /* ... */`
- Imports: zuerst `@TypeScriptImport`, danach `defaultImports` aus YAML
- Header‑Kommentar enthält immer den Java‑Source‑FQN

## Beispielablauf

1) Java‑Klassse annotieren:

```java
@GenerateTypeScript(value = "models", name = "Human")
public class Person {
    @TypeScript(optional = true, description = "age in years")
    private Integer age;
}
```

2) Optional: `java-to-ts.yaml` im Modulverzeichnis anlegen:

```yaml
typeMappings:
  java.time.Instant: Date
defaultImports:
  - "import { Util } from 'utils/Util';"
```

3) Build ausführen:

```bash
mvn -q -pl your-module -am clean generate-resources
```

→ Ergebnis unter `${project.build.directory}/ts-generated/...`

## Bekannte Limitierungen (Stand jetzt)

- Heuristische Follow‑Auflösung (auf Basis von Simple‑Namen, ohne vollständige Importauflösung)
- Vereinfachte Generics‑Unterstützung; komplexe Typausdrücke werden konservativ behandelt
- Kein automatisches Barrel‑`index.ts`/Re‑Exports

## Lizenz / Mitwirken

Beiträge willkommen. Bitte Tests ergänzen und die README aktualisieren, wenn neue Features hinzukommen.
