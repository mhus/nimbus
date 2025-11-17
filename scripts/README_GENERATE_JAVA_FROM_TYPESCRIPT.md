# TypeScript-zu-Java Generator - Dokumentation

## Übersicht

Der `generate-java-from-typescript` Mechanismus ist ein automatisiertes System zur Generierung von Java-Klassen aus TypeScript Interface- und Enum-Definitionen. Dieser Mechanismus stellt sicher, dass die Datenstrukturen zwischen Frontend (TypeScript) und Backend (Java) synchron bleiben.

## Zweck

Der Hauptzweck dieses Mechanismus ist:

1. **Konsistenz**: Eine einzige Wahrheitsquelle (TypeScript-Definitionen) für Datenstrukturen
2. **Automatisierung**: Vermeidung manueller Synchronisation zwischen Frontend und Backend
3. **Typsicherheit**: Reduzierung von Typ-Inkonsistenzen zwischen Client und Server
4. **Wartbarkeit**: Änderungen an Datenstrukturen werden automatisch propagiert

## Komponenten

Der Mechanismus besteht aus mehreren zusammenarbeitenden Komponenten:

### 1. Shell-Script: `generate-java-from-typescript.sh`

**Pfad**: `scripts/generate-java-from-typescript.sh`

**Funktion**: Wrapper-Script für die Generierung

**Verantwortlichkeiten**:
- Umgebungsvariablen und Pfade setzen
- Node.js-Installation prüfen
- Ausgabeverzeichnis vorbereiten
- Alte generierte Dateien bereinigen
- Generator-Script aufrufen
- Benutzerfreundliche Ausgaben und Fehlermeldungen

**Wichtige Konfigurationen**:
```bash
TS_SOURCE_DIR="$PROJECT_ROOT/client/packages/shared/src/types"
JAVA_OUTPUT_DIR="$PROJECT_ROOT/server/generated/src/main/java/de/mhus/nimbus/generated"
```

### 2. Generator-Script: `ts-to-java-generator.js`

**Pfad**: `scripts/ts-to-java-generator.js`

**Funktion**: Kern-Logik für die Transformation von TypeScript zu Java

**Hauptfunktionen**:
- `parseTypeScriptFile()`: Parst TypeScript-Dateien und extrahiert Interfaces und Enums
- `generateJavaClass()`: Generiert Java-Code aus geparsten Daten
- `mapTypeScriptTypeToJava()`: Übersetzt TypeScript-Typen zu Java-Typen
- `boxPrimitiveType()`: Konvertiert primitive Typen zu Wrapper-Typen für Generics

### 3. Maven-Integration

**Pfad**: `server/generated/pom.xml`

**Profil-ID**: `generate-from-typescript`

**Funktion**: Integration in den Maven-Build-Prozess über exec-maven-plugin

## Funktionsweise

### Schritt-für-Schritt-Ablauf

1. **Dateierkennung**:
   - Scannt `client/packages/shared/src/types/` nach `.ts`-Dateien
   - Ignoriert `.d.ts`-Deklarationsdateien

2. **Parsing**:
   - Verwendet reguläre Ausdrücke zur Extraktion von Interfaces und Enums
   - Extrahiert Feldnamen, Typen und optionale Marker
   - Erkennt komplexe Typen wie Arrays und Records

3. **Typ-Mapping**:
   - Übersetzt TypeScript-Typen zu äquivalenten Java-Typen
   - Handhabt primitive Typen, Wrapper-Typen und Custom-Types
   - Konvertiert Arrays zu `List<>` und Records zu `Map<>`

4. **Code-Generierung**:
   - Generiert Java-Klassen mit Lombok-Annotationen
   - Erstellt Enums mit Value-Gettern
   - Fügt Import-Statements automatisch hinzu

5. **Datei-Ausgabe**:
   - Schreibt generierte Klassen in `server/generated/src/main/java/de/mhus/nimbus/generated/`
   - Erstellt eine Java-Datei pro Interface/Enum
   - Fügt Header-Kommentare mit Warnung vor manueller Bearbeitung hinzu

## Typ-Mappings

### Grundtypen

| TypeScript | Java | Bemerkung |
|------------|------|-----------|
| `number` | `double` | Standard-Zahltyp |
| `string` | `String` | Unveränderlicher String |
| `boolean` | `boolean` | Primitiver Boolean |
| `any` | `Object` | Generischer Objekttyp |
| `void` | `void` | Kein Rückgabewert |
| `Date` | `java.time.Instant` | Zeitstempel |

### Komplexe Typen

| TypeScript | Java | Beispiel |
|------------|------|----------|
| `Type[]` | `java.util.List<Type>` | `number[]` → `List<Double>` |
| `Record<K, V>` | `java.util.Map<K, V>` | `Record<string, number>` → `Map<String, Double>` |
| `field?` | Wrapper-Typ | `count?: number` → `Double count` |

### Custom Types

Custom Types (z.B. `Vector3`, `Rotation`) werden direkt übernommen, sofern sie in der TYPE_MAP definiert sind oder als andere generierte Klassen existieren.

## Erweiterbarkeit

### 1. Neue Typ-Mappings hinzufügen

**Datei**: `scripts/ts-to-java-generator.js`

**Konstante**: `TYPE_MAP` (Zeilen 22-34)

**Vorgehen**:
```javascript
const TYPE_MAP = {
  // Bestehende Mappings...
  'Vector3': 'Vector3',
  'Rotation': 'Rotation',
  
  // Neues Mapping hinzufügen:
  'CustomType': 'de.mhus.nimbus.custom.CustomType',
  'UUID': 'java.util.UUID',
  'BigDecimal': 'java.math.BigDecimal',
};
```

**Anwendungsfall**: Wenn neue TypeScript-Typen hinzugefügt werden, die spezielle Java-Typen erfordern.

### 2. Parsing-Logik erweitern

**Funktion**: `parseTypeScriptFile()` (Zeilen 39-134)

**Erweiterungspunkte**:

#### a) Neue TypeScript-Konstrukte parsen

Beispiel: Union Types unterstützen
```javascript
// In parseTypeScriptFile() hinzufügen:
const unionTypeRegex = /type\s+(\w+)\s*=\s*([^;]+);/g;
let unionMatch;
while ((unionMatch = unionTypeRegex.exec(content)) !== null) {
  // Union Type verarbeiten
  result.unionTypes.push({
    name: unionMatch[1],
    types: unionMatch[2].split('|').map(t => t.trim())
  });
}
```

#### b) JSDoc-Kommentare extrahieren

```javascript
// Vor dem Interface-Parsing:
const jsdocRegex = /\/\*\*\s*\n([^*]|\*(?!\/))*\*\//g;
const comments = content.match(jsdocRegex);
// Kommentare zu den generierten Klassen hinzufügen
```

### 3. Code-Generierung anpassen

**Funktion**: `generateJavaClass()` (Zeilen 172-254)

**Erweiterungspunkte**:

#### a) Zusätzliche Lombok-Annotationen

```javascript
// In generateJavaClass():
imports.add('lombok.EqualsAndHashCode');
// ...
javaCode += `@EqualsAndHashCode\n`;
```

#### b) Validierungs-Annotationen hinzufügen

```javascript
// Für Felder:
if (!field.optional) {
  imports.add('jakarta.validation.constraints.NotNull');
  javaCode += `    @NotNull\n`;
}
javaCode += `    private ${field.type} ${field.name};\n`;
```

#### c) Custom-Annotationen für Felder

```javascript
// Für spezielle Feldtypen:
if (field.type === 'String' && field.name.includes('email')) {
  imports.add('jakarta.validation.constraints.Email');
  javaCode += `    @Email\n`;
}
```

### 4. Neue Dateitypen hinzufügen

Aktuell werden nur `.ts`-Dateien verarbeitet. Für `.tsx` oder andere Formate:

```javascript
// In main():
const tsFiles = fs.readdirSync(TS_SOURCE_DIR)
  .filter(file => file.endsWith('.ts') || file.endsWith('.tsx'))
  .filter(file => !file.endsWith('.d.ts'))
  .map(file => path.join(TS_SOURCE_DIR, file));
```

### 5. Ausgabeformat anpassen

**Erweiterung für Records/Immutable Classes**:

```javascript
// Alternative zur @Data-Annotation:
if (interfaceData.isImmutable) {
  javaCode += `@Value\n`;  // Lombok @Value für unveränderliche Klassen
} else {
  javaCode += `@Data\n`;
}
```

### 6. Mehrere Quellverzeichnisse unterstützen

```javascript
const SOURCE_DIRS = [
  path.join(__dirname, '../client/packages/shared/src/types'),
  path.join(__dirname, '../client/packages/shared/src/models'),
  path.join(__dirname, '../client/packages/shared/src/entities')
];

// In main():
for (const sourceDir of SOURCE_DIRS) {
  const files = fs.readdirSync(sourceDir)
    .filter(file => file.endsWith('.ts'))
    .map(file => path.join(sourceDir, file));
  allFiles.push(...files);
}
```

### 7. Vererbung und Interfaces unterstützen

```javascript
// Interface-Erweiterung parsen:
const extendsRegex = /export\s+interface\s+(\w+)\s+extends\s+(\w+)\s*\{/g;
let extendsMatch;
while ((extendsMatch = extendsRegex.exec(content)) !== null) {
  result.interfaces.push({
    name: extendsMatch[1],
    extends: extendsMatch[2],
    fields: [] // ... weitere Felder
  });
}

// In generateJavaClass():
if (interfaceData.extends) {
  javaCode += `public class ${interfaceData.name} extends ${interfaceData.extends} {\n`;
}
```

## Best Practices für Erweiterungen

### 1. Rückwärtskompatibilität wahren

- Bestehende Typ-Mappings nicht ohne Grund ändern
- Neue Features optional machen
- Tests für bestehende Funktionalität beibehalten

### 2. Konfigurierbarkeit

Anstatt fest codierte Werte zu verwenden:

```javascript
// Am Anfang der Datei:
const CONFIG = {
  TS_SOURCE_DIR: process.env.TS_SOURCE_DIR || path.join(__dirname, '../client/packages/shared/src/types'),
  JAVA_OUTPUT_DIR: process.env.JAVA_OUTPUT_DIR || path.join(__dirname, '../server/generated/src/main/java/de/mhus/nimbus/generated'),
  JAVA_PACKAGE: process.env.JAVA_PACKAGE || 'de.mhus.nimbus.generated',
  USE_VALIDATION: process.env.USE_VALIDATION === 'true',
};
```

### 3. Fehlerbehandlung

Robuste Fehlerbehandlung bei Parsing-Fehlern:

```javascript
// In einer Schleife über Dateien:
for (const tsFile of tsFiles) {
  try {
    const parsedData = parseTypeScriptFile(tsFile);
    const javaClasses = generateJavaClass(parsedData, fileName);
    // ...
  } catch (error) {
    console.error(`  ✗ Error processing ${fileName}: ${error.message}`);
    // Optional: Fehlerhafte Datei überspringen statt Abbruch
    continue;
  }
}
```

### 4. Logging und Debugging

Detaillierte Logs für Debugging:

```javascript
const DEBUG = process.env.DEBUG === 'true';

function debug(message) {
  if (DEBUG) {
    console.log(`[DEBUG] ${message}`);
  }
}

// Verwendung:
debug(`Parsed interface: ${interfaceName} with ${fields.length} fields`);
```

### 5. Tests schreiben

Für neue Features sollten Tests hinzugefügt werden:

```javascript
// test/ts-to-java-generator.test.js
const { parseTypeScriptFile, generateJavaClass } = require('../scripts/ts-to-java-generator');

describe('TypeScript to Java Generator', () => {
  it('should parse simple interface', () => {
    const tsCode = `
      export interface User {
        id: number;
        name: string;
      }
    `;
    // Test-Logik
  });
});
```

## Verwendung

### Manueller Aufruf

```bash
# Vom Projekt-Root aus:
./scripts/generate-java-from-typescript.sh
```

### Maven-Integration

```bash
# Vom Projekt-Root:
mvn generate-sources -Pgenerate-from-typescript -pl server/generated

# Vom generated-Modul:
cd server/generated
mvn generate-sources -Pgenerate-from-typescript
```

### In CI/CD-Pipeline

```yaml
# .github/workflows/build.yml
steps:
  - name: Generate Java from TypeScript
    run: |
      ./scripts/generate-java-from-typescript.sh
  
  - name: Build project
    run: mvn clean install
```

## Fehlerbehandlung

### Node.js nicht gefunden

**Problem**: `Node.js not found` Fehlermeldung

**Lösung**:
- macOS: `brew install node`
- Linux: `sudo apt-get install nodejs` oder `sudo yum install nodejs`
- Windows: Download von https://nodejs.org/

### Parsing-Fehler

**Problem**: TypeScript-Datei kann nicht geparst werden

**Lösung**:
1. TypeScript-Datei auf Syntaxfehler prüfen
2. Regex-Patterns im Generator erweitern
3. Komplexe Typen manuell in TYPE_MAP hinzufügen

### Import-Fehler in generierten Klassen

**Problem**: Generierte Java-Klassen kompilieren nicht

**Lösung**:
1. Fehlende Typ-Mappings in TYPE_MAP ergänzen
2. Abhängigkeiten in `server/generated/pom.xml` hinzufügen
3. Custom-Types in separate Module auslagern

## Architektur-Entscheidungen

### Warum TypeScript als Quelle?

- TypeScript ist die Single Source of Truth für Frontend-Typen
- Frontend-Entwickler definieren die Datenstrukturen
- Automatische Synchronisation reduziert Fehler

### Warum Lombok?

- Reduziert Boilerplate-Code in generierten Klassen
- Builder-Pattern für einfache Objekterstellung
- Automatische Getter/Setter, equals, hashCode, toString

### Warum Regular Expressions statt AST-Parser?

- Einfachere Implementation
- Ausreichend für die aktuellen Anforderungen
- Erweiterbar für komplexere Fälle

**Alternative für komplexere Anforderungen**: TypeScript Compiler API verwenden

```javascript
const ts = require('typescript');

function parseWithCompilerAPI(filePath) {
  const program = ts.createProgram([filePath], {});
  const sourceFile = program.getSourceFile(filePath);
  // AST traversieren
}
```

## Wartung und Updates

### Regelmäßige Generierung

Nach folgenden Änderungen neu generieren:
1. TypeScript-Interfaces hinzugefügt, geändert oder entfernt
2. TypeScript-Enums hinzugefügt, geändert oder entfernt
3. Nach Pull von Änderungen, die TypeScript-Typen betreffen

### Versionskontrolle

**Empfehlung**: Generierte Dateien in Git committen

**Vorteile**:
- Änderungen an generierten Klassen sind nachvollziehbar
- Build funktioniert auch ohne Node.js
- Code-Reviews können Änderungen überprüfen

**Nachteile**:
- Merge-Konflikte bei gleichzeitigen Änderungen
- Repository-Größe nimmt zu

### Dokumentation aktualisieren

Bei Erweiterungen des Mechanismus:
1. Diese Datei (`README_GENERATE_JAVA_FROM_TYPESCRIPT.md`) aktualisieren
2. Kommentare im Code ergänzen
3. Beispiele hinzufügen

## Integration mit anderen Modulen

### Abhängigkeit hinzufügen

```xml
<!-- In anderem Server-Modul -->
<dependency>
    <groupId>de.mhus.nimbus</groupId>
    <artifactId>generated</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Verwendung in Java-Code

```java
import de.mhus.nimbus.generated.Vector3;
import de.mhus.nimbus.generated.Rotation;
import de.mhus.nimbus.generated.BlockStatus;

// Objekte erstellen mit Builder-Pattern
Vector3 position = Vector3.builder()
    .x(10.0)
    .y(20.0)
    .z(30.0)
    .build();

// Enums verwenden
BlockStatus status = BlockStatus.OPEN;
int statusValue = status.getValue(); // returns 1

// Optionale Felder
Rotation rotation = Rotation.builder()
    .y(90.0)
    .p(45.0)
    .build(); // r bleibt null
```

## Beispiele für Erweiterungen

### Beispiel 1: JSON-Serialization-Annotationen hinzufügen

```javascript
// In generateJavaClass():
imports.add('com.fasterxml.jackson.annotation.JsonProperty');

for (const field of interfaceData.fields) {
  javaCode += `    @JsonProperty("${field.name}")\n`;
  javaCode += `    private ${field.type} ${field.name};\n`;
}
```

### Beispiel 2: OpenAPI/Swagger-Annotationen

```javascript
imports.add('io.swagger.v3.oas.annotations.media.Schema');

javaCode += `@Schema(description = "${interfaceData.name} data class")\n`;
javaCode += `public class ${interfaceData.name} {\n`;

for (const field of interfaceData.fields) {
  javaCode += `    @Schema(description = "${field.name}", required = ${!field.optional})\n`;
  javaCode += `    private ${field.type} ${field.name};\n`;
}
```

### Beispiel 3: JPA-Entity-Annotationen

```javascript
if (interfaceData.isEntity) {
  imports.add('jakarta.persistence.Entity');
  imports.add('jakarta.persistence.Id');
  imports.add('jakarta.persistence.GeneratedValue');
  
  javaCode += `@Entity\n`;
  javaCode += `public class ${interfaceData.name} {\n`;
  
  for (const field of interfaceData.fields) {
    if (field.name === 'id') {
      javaCode += `    @Id\n`;
      javaCode += `    @GeneratedValue\n`;
    }
    javaCode += `    private ${field.type} ${field.name};\n`;
  }
}
```

## Zusammenfassung

Der `generate-java-from-typescript` Mechanismus ist ein leistungsfähiges und erweiterbares System zur Synchronisation von Datenstrukturen zwischen TypeScript und Java. Durch die modulare Architektur und die klaren Erweiterungspunkte können neue Anforderungen einfach integriert werden, ohne die bestehende Funktionalität zu beeinträchtigen.

**Wichtigste Erweiterungspunkte**:
1. TYPE_MAP für neue Typ-Mappings
2. parseTypeScriptFile() für neue TypeScript-Konstrukte
3. generateJavaClass() für angepasste Java-Code-Generierung
4. Konfigurationskonstanten für Pfade und Optionen

Für weitere Fragen oder Unterstützung bei Erweiterungen, siehe die Inline-Kommentare im Code oder kontaktiere das Entwicklungsteam.
