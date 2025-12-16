
# Java 2 TypeScript Generator

## Setup

[ ] Erstelle in plugins in neues modul "generate-java-to-ts-maven-plugin".
Du kannst dich an dem bereits existierenden plugin "generate-ts-tojava-maven-plugin" orientieren.
- Erstelle nur das projekt mit den projekt dateien.
- Erstelle die Mojo Klasse

[ ] Parameter fuer das Mojo:
- inputDirectory: Verzeichnis in dem die Java Dateien liegen (Standardwert: ${project.basedir}/src/main/java)
- outputDirectory: Verzeichnis in dem die generierten TypeScript Dateien abgelegt werden (Standardwert: ${project.basedir}/src/main/ts-generated)

[ ] Das Mojo soll Konfigurierbar sein, dafuer wird eine datei java-to-ts.yaml geladen in einer Configuration Klasse.

[ ] Erstelle eine Klasse JavaCollector die alle Java Dateien im inputDirectory rekursiv sammelt die an der Klasse
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

[ ] Erstelle eine Klasse TypeScriptGenerator der aus dem JavaClassModel eine TypeScriptModel erzeugt.

[ ] Erstelle einen TypeScriptModelWriter der das TypeScriptModel in eine TypeScript Datei schreibt.

[ ] Im Mojo wird der JavaCollector aufgerufen, der alle Java Klassen sammelt und parst.

