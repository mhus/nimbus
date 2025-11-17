# nimbus

Dieses Repository ist als Maven Multi-Module-Projekt aufgebaut.

Module:
- Root: Aggregator-POM (packaging=pom)
- server: Aggregator für Server-Module (packaging=pom)
  - shared: geteilte Bibliothek (packaging=jar), Paket: de.mhus.nimbus.shared


Problembehebung: IntelliJ erkennt das Projekt nicht als Maven-Projekt

Wenn IntelliJ IDEA das Projekt nicht als Maven-Projekt erkennt, gehen Sie wie folgt vor:

1) Projekt aus der Root-POM importieren
- Datei -> New -> Project from Existing Sources...
- Wählen Sie die Root-Datei pom.xml im Repository-Root aus (nicht die im Unterordner server).
- Import-Option "Maven" wählen und Assistent abschließen.

2) POMs manuell als Maven-Projekt hinzufügen
- Öffnen Sie die Datei pom.xml im Projekt-Root in IntelliJ.
- Im Editor erscheint häufig ein Hinweis "Add as Maven Project" – anklicken.
- Alternativ: View -> Tool Windows -> Maven öffnen. Klicken Sie auf das Plus-Symbol und wählen Sie die Root-pom.xml.

3) Reimport erzwingen
- Tool Window "Maven" -> Reload All Maven Projects (rundpfeil-Icon) klicken.
- Oder: Rechtsklick auf pom.xml -> Maven -> Reload project.

4) JDK 25 korrekt konfigurieren
- Stellen Sie sicher, dass JDK 25 installiert ist (z. B. mit SDKMAN oder JetBrains Runtime nicht ausreichend, da Maven einen JDK benötigt).
- IntelliJ: File -> Project Structure -> SDKs: JDK 25 hinzufügen.
- Project Structure -> Project: Project SDK = JDK 25 und Project language level = 25 (Preview-Features nur bei Bedarf).
- Settings -> Build, Execution, Deployment -> Build Tools -> Maven -> Importing: JDK für Importer = JDK 25.

5) Maven-Import-Einstellungen prüfen
- Settings -> Build, Execution, Deployment -> Build Tools -> Maven:
  - Maven home: "Bundled (Maven 3)" oder eigenes Maven.
  - User settings file: Standard (~/.m2/settings.xml) oder Ihr angepasstes.
  - Automatically download: Sources/Documentation aktivieren, optional.

6) Caches leeren, falls weiterhin Probleme bestehen
- File -> Invalidate Caches... -> Invalidate and Restart.
- Danach erneut die Root-pom.xml importieren.

7) .idea/.iml neu erzeugen (letzter Ausweg)
- IntelliJ schließen.
- Löschen Sie im Projektordner: .idea-Verzeichnis und alle *.iml-Dateien.
- IntelliJ neu öffnen -> "Open" und den Projektordner wählen -> Root-pom.xml als Maven-Projekt importieren.

8) Maven-Build auf der Kommandozeile prüfen
- mvn -v  (zeigt verwendetes JDK und Maven)
- mvn -q -e -DskipTests clean verify
  - Wenn der Build erfolgreich ist, sollte IntelliJ das Projekt ebenfalls importieren können.

Hinweise zur Struktur
- Root pom.xml deklariert <packaging>pom</packaging> und enthält das Modul server.
- server/pom.xml ist ebenfalls ein Aggregator mit dem Untermodul shared.
- Das eigentliche kompilierbare Modul ist derzeit server/shared (packaging=jar).

Typische Stolpersteine
- Falsches JDK (z. B. nur eine JRE): Stellen Sie sicher, dass ein vollständiges JDK 25 verwendet wird.
- Projekt im falschen Ordner geöffnet: Öffnen/Importieren Sie immer die Root-pom.xml.
- Alte Projektmetadaten stören: .idea und *.iml löschen und neu importieren.

Bei weiteren Fragen oder wenn der Import weiterhin scheitert, teilen Sie bitte die genaue IntelliJ-Version, die Ausgabe von `mvn -v` und ggf. Screenshots der Maven-Toolwindow-Ansicht mit.
