
# Java 2 TypeScript Generator

## Setup

[x] Erstelle in plugins in neues modul "generate-java-to-ts-maven-plugin".
Du kannst dich an dem bereits existierenden plugin "generate-ts-tojava-maven-plugin" orientieren.
- Erstelle nur das projekt mit den projekt dateien.
- Erstelle die Mojo Klasse

[x] Parameter fuer das Mojo:
- inputDirectory: Verzeichnis in dem die Java Dateien liegen (Standardwert: ${project.basedir}/src/main/java)
- outputDirectory: Verzeichnis in dem die generierten TypeScript Dateien abgelegt werden (Standardwert: ${project.basedir}/src/main/ts-generated)

[x] Das Mojo soll Konfigurierbar sein, dafuer wird eine datei java-to-ts.yaml geladen in einer Configuration Klasse.

[x] Erstelle eine Klasse JavaCollector die alle Java Dateien im inputDirectory rekursiv sammelt die an der Klasse
"@GenerateTypeScript" annotiert haben.
- Zum parsen der Java Dateien kannst du die Bibliothek JavaParser verwenden: https://javaparser.org/
- Wurde eine Datei gefunden, wird diese direkt an einen JavaParser (Klasse) uebergeben.
- Der javaParser erstellt ein internes Modell der Java Klasse (z.B. JavaClassModel).
- Weitere Annotationen wie @TypeScript(ignore=true) fuer Felder
- Weitere Annotationen an der Klasse @TypeScriptImport({"ts import", ...}) 
  oder an Feldern @TypeScript(import="")
- Weitere Annotationen an der einem Feld @TypeScript(type="ts type")
- Es werden nur Felder beachtet.
- Enum muessen auch beachtet werden.
- Folge einem Typ, wenn an dem Feld @TypeScript(follow=true) angegeben ist.
- Löse generische Typen auf (z.B. List<Type> -> Type[]), Integer - number

- @TypeScript(follow=true, type="ts type", import="", ignore=false, optional=false)
- @GenerateTypeScript(value="subfolder") oder @GenerateTypeScript("subfolder")

[x] Erstelle eine Klasse TypeScriptGenerator der aus dem JavaClassModel eine TypeScriptModel erzeugt.

[x] Erstelle einen TypeScriptModelWriter der das TypeScriptModel in eine TypeScript Datei schreibt.

[x] Im Mojo wird der JavaCollector aufgerufen, der alle Java Klassen sammelt und parst.

[x] Tests hinzugefügt: Beispiel-Java-Dateien mit Annotationen und ein JUnit-Test, der das Mojo ausführt und TS-Dateien unter target/test-output/java2ts erzeugt und prüft.

### Annotationen
- [x] GenerateTypeScript Annotation erstellt: de.mhus.nimbus.tools.generatej2ts.annotations.GenerateTypeScript
  - Nutzung: `@GenerateTypeScript("subfolder")` oder `@GenerateTypeScript(value="subfolder")`
  - Retention: SOURCE, Target: TYPE

- [x] TypeScript Annotation erstellt: de.mhus.nimbus.tools.generatej2ts.annotations.TypeScript
  - Nutzung (auf Feldern): `@TypeScript(optional=true)`, `@TypeScript(ignore=true)`, `@TypeScript(follow=true)`, `@TypeScript(type="ts type")`
  - Zusätzliches Import-Attribut via Alias: `@TypeScript(tsImport="import { Foo } from './Foo';")`
  - Retention: SOURCE, Target: FIELD

- [x] TypeScriptImport Annotation erstellt: de.mhus.nimbus.tools.generatej2ts.annotations.TypeScriptImport
  - Nutzung (auf Klassen/Enums):
    - Einzeln: `@TypeScriptImport("import { ColorHex } from '../types/ColorHex';")`
    - Mehrere: `@TypeScriptImport({"import { A } from './A';", "import { B } from './B';"})`
  - Retention: SOURCE, Target: TYPE

### Konfiguration: typeMappings (Java → TypeScript)

- In der Konfigurationsdatei java-to-ts.yaml kann eine Liste/Map von Typ-Mappings definiert werden, die bei der Generierung angewendet werden.
- Unterstützte Formate:

  Als Map:
  typeMappings:
    java.time.Instant: Date
    Instant: Date

  Als Liste von Objekten:
  typeMappings:
    - java: java.time.Instant
      ts: Date
    - java: Instant
      ts: Date

- Default-Mapping: Instant → Date
  Unabhängig von der Konfiguration wird der Java-Typ Instant (sowohl als SimpleName als auch als FQN java.time.Instant) standardmäßig nach TypeScript Date gemappt.
  Ein Mapping in der Konfiguration kann dies bei Bedarf überschreiben.

[x] In der config soll es eine liste von type mappings geben. Ausserdem soll der java 
Type Instant by default in der liste in Date gemappt werden. diese mappings sollen 
dann beim umstellen zu typescript angewendet werden.

[x] In der config soll es eine Liste von defsult Imports geben die bei TypeScript interface
dateien angelegt werden. Wichtig, wenn der import ein relative pfad (kein @...) ist muss 
automatisch der pfad um ../ erweitert werden je tiefer die datei in der verzeichnis struktur liegt.
Die Ablage ist in @GenerateTypeScript als value angegeben.

### Konfiguration: defaultImports (zusätzliche TS-Imports für Interfaces)

- In der Konfigurationsdatei java-to-ts.yaml kann eine Liste oder ein einzelner String unter dem Schlüssel defaultImports angegeben werden. Diese Import-Zeilen werden an jede generierte TypeScript-Interface-Datei angehängt (Enums werden nicht betroffen).

Beispiele:

  Als Liste:
  defaultImports:
    - "import { Util } from 'utils/Util';"
    - "import something from '@scope/some';"

  Als einzelner String:
  defaultImports: "import { Util } from 'utils/Util';"

- Pfad-Anpassung: Wenn der import-Pfad relativ ist (kein Alias mit @ und nicht beginnend mit . oder /), dann wird er abhängig von der Subfolder-Tiefe des Ziels (aus @GenerateTypeScript("subfolder")) automatisch mit ../ vorangestellt. Tiefe = Anzahl der Ordnersegmente im Subfolder. Beispiel: subfolder="models" → Tiefe 1 → "import { Util } from 'utils/Util'" wird zu "import { Util } from '../utils/Util'".
- Imports, die mit @ oder / beginnen oder bereits mit . beginnen, werden nicht verändert.

[x] Wenn in @GenerateTypeScript der pfad mit einem '.ts' endet, dann wurde nicht nur
der pfad, sonern auch der dateiname angegeben. Das wird beim Einlesen am 
letzten '/' getrennt und der Dateiname entsprechend übernommen. Beispiel:
`@GenerateTypeScript("models/named/Custom.ts")` erzeugt die Datei
`models/named/Custom.ts`, der Interface‑Name bleibt jedoch der Klassenname.

[*] Erweitere @GenerateTypeScript um einen neuen parameter mit dem der name des interfaces
angegeben werden kann. Dieser name wird dann anstelle des default benutzt.

### @GenerateTypeScript: name (Interface-/Enum-Name überschreiben)

- Neues Attribut: `name` (optional)
- Nutzung:
  - `@GenerateTypeScript(value="models", name="Human")`
  - Alternativ weiterhin nur Subfolder oder Dateiname:
    - `@GenerateTypeScript("models")`
    - `@GenerateTypeScript("models/named/Custom.ts")` (setzt expliziten Dateinamen)
- Wirkung:
  - Wenn `name` gesetzt ist, wird der generierte Typ in TypeScript mit diesem Namen erzeugt:
    - `export interface Human { ... }`
  - Der Dateiname ergibt sich standardmäßig aus dem Interface‑Namen (`Human.ts`).
  - Falls in `value` ein expliziter Dateiname mit `.ts` angegeben wurde, hat `name` keinen Einfluss auf den Dateinamen (nur auf den Interface‑Namen).

Beispiel:

@GenerateTypeScript(value="models", name="Human") auf einer Klasse `Renamed`

Ergebnis: Datei `models/Human.ts` mit Inhalt `export interface Human { ... }` und Header `Source: de.example.models.Renamed`.




