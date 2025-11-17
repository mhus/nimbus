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


JWT-Key-Tooling (im Modul server/shared)
- Paket: de.mhus.nimbus.shared.keys
- KeyId: Record mit Feldern owner und uuid (beide Pflicht). Hilfsmethode KeyId.of(owner, uuid) trimmt Eingaben.
- PublicKeyProvider: Interface zum Laden von asymmetrischen Public Keys per KeyId. Rückgabewert Optional<PublicKey>.
- SecretKeyProvider: Interface zum Laden von symmetrischen Secret Keys per KeyId. Rückgabewert Optional<javax.crypto.SecretKey>.
- SyncKeyProvider: Interface speziell für synchrone (symmetrische) Schlüssel. Rückgabewert Optional<javax.crypto.SecretKey>.
- KeyService (@Service): Aggregiert die oben genannten Provider und liefert Keys auf Anfrage. Key-Id-Format ist "owner:uuid".

Gedachter Einsatz in Spring:
- Mehrere Implementierungen der Provider können als Beans registriert werden (z. B. in anderen Modulen/Projekten).
- KeyService injiziert die Provider als ObjectProvider<List<PublicKeyProvider>>, ObjectProvider<List<SecretKeyProvider>> und ObjectProvider<List<SyncKeyProvider>> (lazy). Er iteriert über die Listen und fragt der Reihe nach ab, bis ein Provider den Key liefert.
- Abhängigkeit: Das shared-Modul bringt dafür eine leichte Spring-Abhängigkeit (spring-context) mit.

Beispiel (Nutzung KeyService):
- PublicKey suchen: keyService.findPublicKey("tenantA:550e8400-e29b-41d4-a716-446655440000")
- SecretKey suchen: keyService.findSecretKey("tenantA:550e8400-e29b-41d4-a716-446655440000")
- SyncKey suchen: keyService.findSyncKey("tenantA:550e8400-e29b-41d4-a716-446655440000")


JWT-Service (im Modul server/shared)
- Paket: de.mhus.nimbus.shared.jwt
- JwtService (@Service): Service zur Erstellung und Validierung von JWT-Tokens. Verwendet KeyService zur Auflösung von Signatur- und Verifikationsschlüsseln.

Unterstützte Algorithmen:
- Symmetrisch (HMAC): Verwendet SecretKey oder SyncKey
- Asymmetrisch (RSA/ECDSA): Verwendet PublicKey zur Validierung

Methoden zur Token-Erstellung:
- createTokenWithSecretKey(keyId, subject, claims, expiresAt): Erstellt JWT mit SecretKey (HMAC)
- createTokenWithSyncKey(keyId, subject, claims, expiresAt): Erstellt JWT mit SyncKey (HMAC)

Methoden zur Token-Validierung:
- validateTokenWithSecretKey(token, keyId): Validiert JWT mit SecretKey, gibt Optional<Jws<Claims>> zurück
- validateTokenWithSyncKey(token, keyId): Validiert JWT mit SyncKey, gibt Optional<Jws<Claims>> zurück
- validateTokenWithPublicKey(token, keyId): Validiert JWT mit PublicKey (asymmetrisch), gibt Optional<Jws<Claims>> zurück

Beispiel (Nutzung JwtService):
```java
// Token erstellen
String token = jwtService.createTokenWithSyncKey(
    "tenantA:550e8400-e29b-41d4-a716-446655440000",
    "user123",
    Map.of("role", "admin", "email", "user@example.com"),
    Instant.now().plusSeconds(3600)
);

// Token validieren
Optional<Jws<Claims>> jws = jwtService.validateTokenWithSyncKey(token, "tenantA:550e8400-e29b-41d4-a716-446655440000");
if (jws.isPresent()) {
    Claims claims = jws.get().getPayload();
    String subject = claims.getSubject();
    String role = claims.get("role", String.class);
}
```

Abhängigkeiten:
- io.jsonwebtoken:jjwt-api:0.12.6 (compile)
- io.jsonwebtoken:jjwt-impl:0.12.6 (runtime)
- io.jsonwebtoken:jjwt-jackson:0.12.6 (runtime)
- org.slf4j:slf4j-api (für Logging via Lombok @Slf4j)


Sign-Service (im Modul server/shared)
- Paket: de.mhus.nimbus.shared.sign
- SignService (@Service): Service zum Signieren und Validieren von Text mittels kryptografischer Schlüssel aus dem KeyService.

Signatur-Format:
- Die Signatur wird als String im Format "keyId:algorithm:signatureBase64" codiert.
- keyId: Schlüssel-Identifikator im Format "owner:uuid"
- algorithm: Verwendeter kryptografischer Algorithmus (z. B. HmacSHA256)
- signatureBase64: Base64-kodierte Signatur-Bytes

Methoden:
- sign(text, keyId): Signiert den Text mit einem symmetrischen Schlüssel. Verwendet automatisch den konstanten Standard-Algorithmus (HmacSHA256). Gibt die vollständige Signatur-Zeichenkette zurück.
- validate(text, signatureString): Validiert den Text gegen eine Signatur-Zeichenkette. Extrahiert keyId, Algorithmus und Signatur aus dem String und überprüft die Signatur. Gibt true zurück, wenn die Signatur gültig ist.

Funktionsweise:
- Beim Signieren wird automatisch der Standard-Algorithmus (HmacSHA256) verwendet.
- Die KeyId, der Algorithmus und die Signatur werden in einem einzigen String kodiert.
- Beim Validieren werden diese Komponenten wieder extrahiert und zur Verifikation verwendet.
- Der Service versucht zuerst einen SyncKey zu finden, dann einen SecretKey.

Beispiel (Nutzung SignService):
```java
// Text signieren
String text = "Important message to sign";
String signature = signService.sign(text, "tenantA:550e8400-e29b-41d4-a716-446655440000");
// signature enthält: "tenantA:550e8400-e29b-41d4-a716-446655440000:HmacSHA256:dGVzdHNpZ25hdHVyZQ=="

// Signatur validieren
boolean isValid = signService.validate(text, signature);
if (isValid) {
    System.out.println("Signatur ist gültig");
} else {
    System.out.println("Signatur ist ungültig");
}
```

Exception:
- SignService.SignatureException: Wird geworfen, wenn kein passender Schlüssel gefunden wird oder die Signierung fehlschlägt.


Cipher-Service (im Modul server/shared)
- Paket: de.mhus.nimbus.shared.cipher
- CipherService (@Service): Service zum Verschlüsseln und Entschlüsseln von Text mittels kryptografischer Schlüssel aus dem KeyService.

Cipher-Format:
- Der Cipher wird als String im Format "keyId:algorithm:encryptedDataBase64:ivBase64" codiert.
- keyId: Schlüssel-Identifikator im Format "owner:uuid"
- algorithm: Verwendeter kryptografischer Algorithmus (z. B. AES/GCM/NoPadding)
- encryptedDataBase64: Base64-kodierte verschlüsselte Daten
- ivBase64: Base64-kodierter Initialisierungsvektor (IV)

Methoden:
- encrypt(text, keyId): Verschlüsselt den Text mit einem symmetrischen Schlüssel. Verwendet automatisch den konstanten Standard-Algorithmus (AES/GCM/NoPadding). Gibt die vollständige Cipher-Zeichenkette zurück.
- decrypt(cipherString): Entschlüsselt eine Cipher-Zeichenkette. Extrahiert keyId, Algorithmus, verschlüsselte Daten und IV automatisch aus dem String. Gibt den entschlüsselten Text zurück.

Funktionsweise:
- Beim Verschlüsseln wird automatisch der Standard-Algorithmus (AES/GCM/NoPadding) verwendet.
- Ein zufälliger 12-Byte-IV wird für jede Verschlüsselung generiert.
- GCM-Modus bietet authentifizierte Verschlüsselung (AEAD) mit 128-Bit Tag-Länge.
- Die KeyId, der Algorithmus, die verschlüsselten Daten und der IV werden in einem einzigen String kodiert.
- Beim Entschlüsseln werden diese Komponenten automatisch extrahiert und zur Dechiffrierung verwendet.
- Der Service versucht zuerst einen SyncKey zu finden, dann einen SecretKey.

Beispiel (Nutzung CipherService):
```java
// Text verschlüsseln
String text = "Sensitive data to encrypt";
String cipher = cipherService.encrypt(text, "tenantA:550e8400-e29b-41d4-a716-446655440000");
// cipher enthält: "tenantA:550e8400-e29b-41d4-a716-446655440000:AES/GCM/NoPadding:dGVzdGVuY3J5cHRlZGRhdGE=:cmFuZG9taXY="

// Cipher entschlüsseln (keyId wird automatisch aus dem Cipher extrahiert)
String decrypted = cipherService.decrypt(cipher);
System.out.println(decrypted); // "Sensitive data to encrypt"
```

Exception:
- CipherService.CipherException: Wird geworfen, wenn kein passender Schlüssel gefunden wird, die Verschlüsselung/Entschlüsselung fehlschlägt oder das Cipher-Format ungültig ist.


Hash-Service (im Modul server/shared)
- Paket: de.mhus.nimbus.shared.hash
- HashService (@Service): Service zum Hashen und Validieren von Strings mit optionaler Salt-Unterstützung.

Hash-Format:
- Einfaches Hashing: "algorithm:hashBase64"
- Hashing mit Salt: "algorithm:salt:hashBase64"
- algorithm: Verwendeter Hash-Algorithmus (z. B. SHA-256)
- salt: Salt-Wert (nur bei Hash mit Salt)
- hashBase64: Base64-kodierter Hash

Methoden:
- hash(text): Hasht den Text mit dem Standard-Algorithmus (SHA-256). Gibt den Hash-String im Format "algorithm:hashBase64" zurück.
- hash(text, salt): Hasht den Text mit einem Salt. Gibt den Hash-String im Format "algorithm:salt:hashBase64" zurück.
- validate(text, hashString): Validiert den Text gegen einen Hash-String (ohne Salt). Gibt true zurück, wenn der Hash gültig ist.
- validate(text, salt, hashString): Validiert den Text gegen einen Hash-String mit Salt. Gibt true zurück, wenn der Hash gültig ist.

Funktionsweise:
- Verwendet SHA-256 als sicheren Standard-Hash-Algorithmus.
- Beim Hashen mit Salt wird der Salt vor dem Text in den Hash einbezogen.
- Der Algorithmus und ggf. der Salt werden im Hash-String gespeichert.
- Bei der Validierung werden Algorithmus und Salt automatisch aus dem Hash-String extrahiert.
- Verwendet MessageDigest.isEqual() für sichere Vergleiche (constant-time comparison).

Beispiel (Nutzung HashService):
```java
// Einfaches Hashing
String text = "myPassword123";
String hash = hashService.hash(text);
// hash enthält: "SHA-256:XohImNooBHFR0OVvjcYpJ3NgPQ1qq73WKhHvch0VQtg="

// Hash validieren
boolean isValid = hashService.validate("myPassword123", hash);
System.out.println(isValid); // true

// Hashing mit Salt
String salt = "randomSalt123";
String hashWithSalt = hashService.hash(text, salt);
// hashWithSalt enthält: "SHA-256:randomSalt123:8Ry7pVBfZvZ8h+cQqH3FQHLp5mY="

// Hash mit Salt validieren
boolean isValidWithSalt = hashService.validate(text, salt, hashWithSalt);
System.out.println(isValidWithSalt); // true
```

Exception:
- HashService.HashException: Wird geworfen, wenn das Hashen fehlschlägt oder das Hash-Format ungültig ist.


Base64-Service (im Modul server/shared)
- Paket: de.mhus.nimbus.shared.base64
- Base64Service (@Service): Service zum Kodieren und Dekodieren von Strings zu/von Base64.

Methoden:
- encode(text): Kodiert den Text in Base64. Gibt den Base64-kodierten String zurück.
- decode(base64Text): Dekodiert einen Base64-String zurück zum Original-Text. Gibt den dekodierten Text zurück.

Funktionsweise:
- Verwendet UTF-8-Kodierung für die String-zu-Byte-Konvertierung.
- Verwendet Java's Standard Base64 Encoder/Decoder.
- Wirft eine Exception bei ungültigen Base64-Eingaben beim Dekodieren.

Beispiel (Nutzung Base64Service):
```java
// Text zu Base64 kodieren
String text = "Hello, World!";
String encoded = base64Service.encode(text);
System.out.println(encoded); // "SGVsbG8sIFdvcmxkIQ=="

// Base64 zu Text dekodieren
String decoded = base64Service.decode(encoded);
System.out.println(decoded); // "Hello, World!"

// Ungültige Base64-Eingabe
try {
    base64Service.decode("invalid!!!base64");
} catch (Base64Service.Base64Exception e) {
    System.out.println("Dekodierung fehlgeschlagen: " + e.getMessage());
}
```

Exception:
- Base64Service.Base64Exception: Wird geworfen, wenn das Kodieren/Dekodieren fehlschlägt oder die Base64-Eingabe ungültig ist.
