
## Rules

- Nutze Java 25, Spring Boot 3, JPA, Spring Security
- Nutze lombok annotations, JPA mit mongoDB
- Nutze Apache Commons Libraries
- Schreibe, wenn gefordert, Unit Tests mit JUnit 5, Mockito, AssertJ
- Es wir zwischen Unit Tests (mvn test) und Integration Tests (/src/integration-test/java, mvn verify) unterschieden
- Es wird immer zwischen Inbound-/Outbound- und Business-Logik unterschieden. 
  - RestController sind inbound: Prüfen die Parameter und geben an einen Service zur Verarbeitung weiter
  - JPA Repositories sind in/outbound, sie werden von einem Service controlliert
  - Services sind Business Logik
  - Für Outbound REST wird ein 'Client' Service erstellt, nur er darf als Client mit der remote REST API interagieren
- Folge Clean Code Prinzipien
- **KEINE HACKS!** Saubere Architektur mit DTOs, Services und klaren Verantwortlichkeiten. Keine JSON-Manipulation oder deepCopy-Tricks. Nutze typisierte Klassen und Builder Patterns
- Im World Umfeld wird noch redis (nur world-* module) für Messaging, Locking und auch Caching verwendet (session related ist oft direkt an der Session im Speicher, nicht in redis)
- In World Player ist Performance wichtig
- Im Modul 'generated' liegen generierte Klassen, die nicht geändert werden können, sie sind der Contract zum Frontend und müssen mit EngineMapper (Service) de/serialisiert werden
- Die Module universe, region, world-* werden als Kubernetes Pods geplant und gestartet. Nur world-player hat an der WebSocket Session einen internen state. Sobald die Socket abbricht, ist dieser ungültig
- Source Code und Kommentare in Englisch
- Spreche sonst Deutsch mit mir
- Es müssen nicht immer alle Compile Fehler und Tests gefixt werden. Reporte Probleme
- Konzentriere die auf die aktuelle Aufgabe und löse diese Schritt für Schritt
