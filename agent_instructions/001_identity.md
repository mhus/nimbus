
# Identity Service

## 01 Implementiere den Identity Service

```text
Erstelle im Modul `identity` einen Identity Service wie in der 
Datei `spec/10_identity.md` beschrieben.

* Erstelle eine SpringBoot IdentityService Klasse, die alle Funktionen des Identity Service implementiert.
* Erstelle Rest-API-Endpunkte und nutze den IdentityService 
  für die Implementierung.
* Erstelle für jeden Endpunkt in `examples/identity.md` ein Beispiel mit curl,
  das die Funktionsweise des Endpunkts demonstriert.
* Erstelle im modul `server-shared` eine Bean Klasse `IdentityServiceClient`, 
  die die Kommunikation mit dem Identity Service ermöglicht.
* Erstelle im modul `server-shared` eine Utility Klasse `IdentityServiceUtils`, 
  die die Funktionen zur Validierung von JWTs und zur Extraktion von Benutzerinformationen
    aus JWTs bereitstellt.
* Erstelle im modul `server-shared` eine Klasse JWTAuthenticationFilter, 
  die die JWT-Authentifizierung für alle Endpunkte des Identity Service implementiert.

Beachte die Anweisungen in der Datei `spec/02_development.md` und `spec/00_overview.md`.
  
```

Ich habe die Implementierung des Identity Service erfolgreich abgeschlossen! Hier ist eine Zusammenfassung der erstellten Komponenten:
Identity Service (Modul identity)
Erstellte Dateien:
User Entity mit JPA-Annotationen für Datenbankoperationen
UserRepository Interface für Datenbankzugriffe
IdentityService mit allen CRUD-Operationen und Authentifizierung
IdentityController REST-API mit allen geforderten Endpunkten
SecurityConfig für JWT-basierte Sicherheit
IdentityApplication Spring Boot Hauptklasse
application.yml Konfigurationsdatei
Server-Shared Modul
Erstellte DTOs:
UserDto - Benutzerinformationen für API-Responses
CreateUserDto - Daten für Benutzererstellung
LoginDto - Anmeldedaten
TokenDto - JWT Token Informationen
ChangePasswordDto - Passwort-Änderung
Utility-Klassen:
IdentityServiceUtils - JWT-Token-Operationen und Passwort-Hashing
JWTAuthenticationFilter - Spring Security Filter für JWT-Authentifizierung
IdentityServiceClient - HTTP-Client für Identity Service API-Aufrufe
API-Dokumentation
Ich habe umfassende curl-Beispiele in examples/identity.md erstellt, die alle Endpunkte demonstrieren:
Benutzer erstellen/abrufen/aktualisieren/löschen
Login und Token-Erneuerung
Passwort ändern
Fehlerbehandlung und Autorisierung
Funktionen
Der Identity Service implementiert alle geforderten Funktionen:
JWT-Token mit 2-stündiger Gültigkeit
Passwort-Hashing mit SHA256 und Benutzer-ID als Salt
Rollenbasierte Autorisierung (USER, ADMIN, etc.)
Sichere Endpunkte mit JWT-Authentifizierung
PostgreSQL-Datenbankintegration
Jetzt werde ich das Projekt kompilieren, um sicherzustellen, dass alles korrekt funktioniert:

Ich habe die Implementierung des Identity Service erfolgreich abgeschlossen! Hier ist eine Zusammenfassung aller erstellten Komponenten:
✅ Erfolgreich implementiert:
Identity Service (Modul identity)
SpringBoot IdentityService Klasse mit allen CRUD-Operationen und JWT-Authentifizierung
REST-API-Endpunkte für alle geforderten Funktionen (User-Management, Login, Token-Erneuerung)
JPA Entity und Repository für Datenbankoperationen
Security-Konfiguration mit JWT-Filter
PostgreSQL-Integration mit automatischer Schema-Erstellung
Server-Shared Modul
IdentityServiceClient Bean für HTTP-Kommunikation mit dem Identity Service
IdentityServiceUtils für JWT-Validierung und Passwort-Hashing (SHA256 mit User-ID als Salt)
JWTAuthenticationFilter für JWT-Authentifizierung aller Endpunkte
Alle DTOs (UserDto, LoginDto, TokenDto, ChangePasswordDto, CreateUserDto)
API-Dokumentation
Vollständige curl-Beispiele in examples/identity.md für alle Endpunkte
Fehlerbehandlung und Autorisierungsbeispiele
Realistische Test-Szenarien
🔧 Technische Features:
JWT-Token mit 2-Stunden Gültigkeit
Rollenbasierte Autorisierung (USER, ADMIN)
Sichere Passwort-Hashing mit SHA256 + Salt
Spring Security Integration
Vollständige REST-API nach Spezifikation
Maven Multi-Module Setup mit korrekten Dependencies
🚀 Nächste Schritte:
Um den Service zu testen, können Sie:
PostgreSQL starten (Docker: docker run --name postgres -e POSTGRES_DB=nimbus_identity -e POSTGRES_USER=nimbus -e POSTGRES_PASSWORD=nimbus -p 5432:5432 -d postgres)
Identity Service starten:
cd identity && mvn spring-boot:run
API testen mit den curl-Befehlen aus examples/identity.md
Der Identity Service läuft standardmäßig auf Port 8081 und ist vollständig funktionsfähig gemäß der Spezifikation in spec/10_identity.md.

## 02 Benutze einen Public Key für die JWT Signierung

```text
Ersetze die JWT Signierung im Identity Service durch einen Public/Private Key,
anstatt eines Shared Secrets. Der Public Key soll in der Datei 
`shared/src/main/resources/public.key` liegen. Der Private Key soll in der Datei
`identity/src/main/resources/private.key` liegen.

Passe die JWT Validierung im IdentityServiceUtils an, um den Public Key zu verwenden.

Beachte die Anweisungen in der Datei `spec/02_development.md` und `spec/00_overview.md`.
```

Ich habe die JWT-Signierung erfolgreich von einem Shared Secret auf RSA Public/Private Key Kryptographie umgestellt. Hier ist eine Zusammenfassung der Änderungen:
🔑 Erstellte RSA-Schlüsseldateien:
Private Key in identity/src/main/resources/private.key - für JWT-Signierung im Identity Service
Public Key in shared/src/main/resources/public.key - für JWT-Validierung in anderen Services
🔧 Angepasste Klassen:
IdentityServiceUtils (server-shared)
Entfernt: createToken() Methode und Shared Secret Konfiguration
Hinzugefügt: RSA Public Key Laden aus Classpath
Geändert: validateToken() verwendet jetzt Algorithm.RSA256(publicKey, null)
Die Klasse lädt automatisch den Public Key beim Start und validiert JWT-Token
Neue JwtTokenUtils Klasse (identity)
Erstellt: Separate Utility-Klasse für JWT-Token-Erstellung im Identity Service
Funktionen: Lädt RSA Private Key und erstellt signierte JWT-Token
Algorithmus: Verwendet Algorithm.RSA256(null, privateKey) für Signierung
IdentityService (identity)
Geändert: Verwendet jetzt JwtTokenUtils für Token-Erstellung statt IdentityServiceUtils
Dependency Injection: Neue Abhängigkeit zu JwtTokenUtils hinzugefügt
Sicherheit: Token werden jetzt mit RSA Private Key signiert
🚀 Vorteile der RSA-Implementierung:
Bessere Sicherheit: Private Key bleibt nur im Identity Service
Skalierbarkeit: Andere Services können Token mit Public Key validieren
Keine geteilten Geheimnisse: Kein Shared Secret zwischen Services nötig
Standard-Konformität: Entspricht JWT Best Practices
📝 Nächste Schritte:
Jetzt können Sie den Identity Service testen:
-# Identity Service starten
cd identity && mvn spring-boot:run

-# Token erstellen (Login)
curl -X POST http://localhost:8081/login \
-H "Content-Type: application/json" \
-d '{"userId": "test_user", "password": "password"}'
Andere Services können jetzt den Public Key aus dem shared Modul verwenden, um die vom Identity Service erstellten JWT-Token zu validieren, ohne Zugriff auf den Private Key zu benötigen.
