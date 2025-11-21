REST-API Beispiele (Universe Modul)

Dieser Leitfaden zeigt praktische curl-Beispiele für die im Universe‑Modul verfügbaren REST‑Endpunkte. Alle Endpunkte sind per JWT (Bearer Token) geschützt. Für die User‑ und Keys‑Verwaltung ist die Rolle ADMIN erforderlich.

Voraussetzungen
- Laufender Server (Beispiel: http://localhost:9040)
- Gültiges JWT im Authorization‑Header (Bearer Token)
- Für die Beispiele werden Shell‑Variablen genutzt

Swagger / OpenAPI
- Swagger UI: http://localhost:9040/swagger-ui/index.html
- OpenAPI JSON: http://localhost:9040/v3/api-docs
- Hinweis: In der Swagger UI können Sie oben rechts über den Button "Authorize" ein JWT eintragen (Scheme: Bearer). Danach werden die geschützten Endpunkte mit dem gesetzten Token aufgerufen.

Vorbereitung (Beispielvariablen)
```
HOST="http://localhost:9040"
TOKEN="<JWT_TOKEN_HIER_EINFÜGEN>"
AUTH_HEADER="Authorization: Bearer $TOKEN"
CT_JSON="Content-Type: application/json"
```

Hinweis zu Authentifizierung
- Der JWT‑Filter ist für folgende Pfade aktiv:
  - /universum/user/* (u. a. User‑Verwaltung)
  - /universe/user/* (u. a. Keys‑Verwaltung)
- Die Beispiele setzen ein gültiges Token voraus. Ersetzen Sie $TOKEN entsprechend.


0) Authentifizierung (ULoginController)
- Basis‑Pfad: /universe/user/auth
- Endpunkte: POST /login, GET /logout, POST /refresh

0.1 Login (Token erhalten)
Hinweis: Für den Login wird KEIN Authorization‑Header benötigt. Content‑Type muss application/json sein.
```
curl -s -X POST \
  -H "$CT_JSON" \
  -d '{
        "username": "alice",
        "password": "SicheresPasswort123!"
      }' \
  "$HOST/universe/user/auth/login"
```
Beispiel, um das Token in die Variable TOKEN zu schreiben (jq):
```
TOKEN=$(curl -s -X POST -H "$CT_JSON" -d '{"username":"alice","password":"SicheresPasswort123!"}' \
  "$HOST/universe/user/auth/login" | jq -r .token)
AUTH_HEADER="Authorization: Bearer $TOKEN"
```
Typische Antwort (200):
```
{
  "token": "<JWT>",
  "userId": "<ID>",
  "username": "alice"
}
```

0.2 Logout (stateless)
Hinweis: Der Server hält keinen Zustand; der Endpunkt dient nur dem Client‑Flow. Kein Token erforderlich.
```
curl -s "$HOST/universe/user/auth/logout" -i
```

0.3 Token erneuern (Refresh)
Hinweis: Erfordert ein gültiges Bearer‑Token im Authorization‑Header.
```
curl -s -X POST \
  -H "$AUTH_HEADER" \
  "$HOST/universe/user/auth/refresh"
```
Auch hier können Sie das neue Token mit jq extrahieren:
```
TOKEN=$(curl -s -X POST -H "$AUTH_HEADER" "$HOST/universe/user/auth/refresh" | jq -r .token)
AUTH_HEADER="Authorization: Bearer $TOKEN"
```

Antwortcodes (typisch)
- 200 OK: Login erfolgreich / Refresh erfolgreich / Logout bestätigt
- 400 Bad Request: Ungültige Anfrage (z. B. fehlende Felder)
- 401 Unauthorized: Falsche Zugangsdaten oder ungültiges/fehlendes Token (bei Refresh)


1) User‑Verwaltung (UUserController)
- Basis‑Pfad: /universum/user/user
- Nur mit Rolle ADMIN

1.1 Liste abrufen (optional filtern)
```
curl -s \
  -H "$AUTH_HEADER" \
  "$HOST/universum/user/user?username=alice&email=alice@example.org&role=ADMIN"
```

1.2 Einzelnen User abrufen
```
USER_ID="<ID>"
curl -s \
  -H "$AUTH_HEADER" \
  "$HOST/universum/user/user/$USER_ID"
```

1.3 User anlegen
Hinweis: Ein Passwort wird standardmäßig NICHT gesetzt. Sie können beim Anlegen optional das Feld "password" mitschicken. Ohne Passwort ist kein Login mit diesem User möglich, bis ein Passwort gesetzt wurde.
```
curl -s -X POST \
  -H "$AUTH_HEADER" -H "$CT_JSON" \
  -d '{
        "username": "alice",
        "email": "alice@example.org",
        "roles": "USER,ADMIN",
        "password": "SicheresPasswort123!"
      }' \
  "$HOST/universum/user/user"
```

1.4 User ändern
```
USER_ID="<ID>"
curl -s -X PUT \
  -H "$AUTH_HEADER" -H "$CT_JSON" \
  -d '{
        "username": "alice2",
        "email": "alice2@example.org",
        "roles": "USER",
        "password": "NeuesSicheresPasswort456!"  # optional: Passwort bei Update setzen
      }' \
  "$HOST/universum/user/user/$USER_ID"
```

1.4.1 Nur Passwort ändern (separater Endpunkt)
```
USER_ID="<ID>"
curl -s -X PUT \
  -H "$AUTH_HEADER" -H "$CT_JSON" \
  -d '{
        "password": "NochEinAnderesPasswort789!"
      }' \
  "$HOST/universum/user/user/$USER_ID/password" -i
```

1.5 User löschen
```
USER_ID="<ID>"
curl -s -X DELETE \
  -H "$AUTH_HEADER" \
  "$HOST/universum/user/user/$USER_ID" -i
```

Antwortcodes (typisch)
- 200 OK: Anfrage erfolgreich (GET/PUT)
- 201 Created: Ressource erstellt (POST)
- 204 No Content: Erfolgreich gelöscht (DELETE)
- 400 Bad Request: Validierungsfehler (z. B. doppelte E‑Mail/Username)
- 404 Not Found: Ressource nicht vorhanden
- 401 Unauthorized: Fehlendes/ungültiges Token


2) Schlüssel‑Verwaltung (UKeysController)
- Basis‑Pfad: /universe/user/keys
- Nur mit Rolle ADMIN

2.1 Liste/Filter
Optionale Query‑Parameter: type, kind, name, algorithm
```
curl -s \
  -H "$AUTH_HEADER" \
  "$HOST/universe/user/keys?type=asymmetric&kind=ssh&algorithm=rsa&name=my-key"
```

2.2 Schlüssel anlegen
Body‑Felder: type, kind, algorithm, name, key
```
curl -s -X POST \
  -H "$AUTH_HEADER" -H "$CT_JSON" \
  -d '{
        "type": "asymmetric",
        "kind": "ssh",
        "algorithm": "rsa",
        "name": "my-key",
        "key": "<KEY_MATERIAL_NICHT_WIRD_NICHT_ZURÜCKGELIEFERT>"
      }' \
  "$HOST/universe/user/keys"
```

2.3 Schlüsselnamen aktualisieren
```
KEY_ID="<ID>"
curl -s -X PUT \
  -H "$AUTH_HEADER" -H "$CT_JSON" \
  -d '{
        "name": "my-key-renamed"
      }' \
  "$HOST/universe/user/keys/$KEY_ID"
```

2.4 Schlüssel löschen
```
KEY_ID="<ID>"
curl -s -X DELETE \
  -H "$AUTH_HEADER" \
  "$HOST/universe/user/keys/$KEY_ID" -i
```

Antwortcodes (typisch)
- 200 OK: Anfrage erfolgreich (GET/PUT)
- 201 Created: Ressource erstellt (POST)
- 204 No Content: Erfolgreich gelöscht (DELETE)
- 404 Not Found: Ressource nicht vorhanden
- 401 Unauthorized: Fehlendes/ungültiges Token


Zusätzliche Hinweise
- Rollenverwaltung: Für User wird das Rollenfeld als kommaseparierte Liste übergeben (z. B. "USER,ADMIN").
- Eindeutigkeit: username und email müssen eindeutig sein; sonst 400 Bad Request.
- Sicherheit: Stellen Sie sicher, dass $TOKEN zur passenden Key‑ID/Signatur des Servers passt. Der Login‑Endpunkt ist projektabhängig; diese Datei nimmt ein bereits vorliegendes Token an.
 - Passwörter: Wird beim Anlegen kein "password" mitgegeben, bleibt das Passwort leer (kein Login möglich). Beim Anlegen mit "password" wird dieses serverseitig sicher gehasht gespeichert (mit User‑ID als Salt).
 - Passwort ändern: Das Passwort kann entweder im allgemeinen Update (PUT /{id}) via Feld "password" gesetzt werden oder separat über PUT /{id}/password. Passwörter werden nie in Antworten zurückgegeben.
