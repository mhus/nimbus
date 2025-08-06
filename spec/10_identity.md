
# Identity Service Spezifikation

## Einleitung

Der Identity Service hat Informationen über Benutzer und deren Rollen.

## Datenmodelle

### Benutzer

```json
{
  "id": "string", // Eindeutige ID des Benutzers - login name
  "name": "string", // Vollständiger Name des Benutzers
  "nickname": "string", // Nickname des Benutzers
  "email": "string", // E-Mail-Adresse des Benutzers
  "roles": ["string"], // Liste der Rollen des Benutzers
  "created_at": "long", // Zeitstempel der Erstellung des Benutzers (Unix-Zeit)
  "updated_at": "long", // Zeitstempel der letzten Aktualisierung des Benutzers (Unix-Zeit)
  "password_hash": "string" // Hash des Passworts des Benutzers (sha256), salt ist die user ID
}
```

## JWT Token

Beim Login mit user und passwort wird ein JWT Token generiert, 
der die Benutzerinformationen und Rollen enthält. Dieser Token 
wird für die Authentifizierung bei anderen Diensten verwendet.

Ein Token kann auch neu erstellt werden. Damit kann die Session
des Benutzers verlängert werden, ohne dass er sich erneut anmelden muss.

Ein Token läuft nach 2 Stunden ab. Der Benutzer muss sich dann erneut anmelden
oder den Token vorher erneuern.

### Token Struktur

```json
{
  "iss": "string", // Aussteller des Tokens (z.B. Identity Service)
  "sub": "string", // Subjekt des Tokens (Benutzer-ID)
  "exp": "long", // Ablaufzeit des Tokens (Unix-Zeit)
  "iat": "long", // Ausstellungszeit des Tokens (Unix-Zeit)
  "roles": ["string"] // Rollen des Benutzers
}
```

## API Endpunkte

### Benutzer anlegen

**POST /users**

Erstellt einen neuen Benutzer.

### Benutzer abfragen

**GET /users/{id}**

Liefert die Informationen über einen Benutzer.

### Benutzer aktualisieren

**PUT /users/{id}**

Aktualisiert die Informationen über einen Benutzer.

### Benutzer löschen

**DELETE /users/{id}**

Löscht einen Benutzer.

### Benutzer auflisten

**GET /users**

Liefert eine Liste aller Benutzer.

### Login

**POST /login**

Erstellt ein JWT Token für einen Benutzer.

### Token erneuern

**POST /token/renew**

Erneuert ein bestehendes JWT Token.

### Change Passwort

**POST /users/{id}/change-password**

Ändert das Passwort eines Benutzers. Der neue Hash wird mit dem alten Passwort-Hash verglichen.

## Shared Tooling

Im `server-shared` Projekt befinden sich Tools zum Validieren von 
JWT Tokens und zum Erstellen von Hashes für Passwörter. Diese 
Tools können in anderen Projekten verwendet werden, um die 
Sicherheit und Konsistenz zu gewährleisten.

Eine Klasse `JwtFilter` ist im `server-shared` Projekt enthalten,
die als Filter für die Rest-API verwendet werden kann. Die Klasse
überprüft den JWT Token in den HTTP-Headern und stellt sicher,
dass der Benutzer authentifiziert ist. Sie extrahiert auch die
Benutzerinformationen und Rollen aus dem Token und speichert sie im
HTTP-Request, damit sie in den Controllern und Services verwendet werden können.
