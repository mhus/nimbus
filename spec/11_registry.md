
# Registry Service Spezifikation

## Einleitung

Der Registry Service verwaltet Informationen über die verfügbaren Welten und deren 
Eigenschaften. Er ermöglicht es, Welten zu registrieren, zu aktualisieren, zu 
löschen und aufzulisten. Der Service bietet auch Funktionen zum Suchen von 
Welten basierend auf verschiedenen Kriterien.

- Er speichert Welten-Metadaten in einer PostgreSQL-Datenbank via JPA.
- Er bietet REST-APIs für die Kommunikation mit anderen Komponenten.
- Er ermöglicht das Abfragen von Welten und deren Metadaten.

## JPA-Entity

```json
{
  "RegistryWorld": {
    "id": "string", // Eindeutige ID der Welt, UUID wird beim anlegen generiert
    "name": "string", // Name der Welt
    "description": "string", // Beschreibung der Welt
    "created_at": long, // Erstellungszeit der Welt (Unix-Zeit)
    "updated_at": long, // Letzte Aktualisierung der Welt (Unix-Zeit)
    "owner_id": "string", // ID des Besitzers der Welt, UUID des Users
    "enabled": boolean, // Gibt an, ob die Welt aktiv ist
    "access_url": "string", // URL der Welt für den Zugriff auf die Welt WebSocket
    "properties": { // Zusätzliche Eigenschaften der Welt
      "key": "string",
      "value": "string"
    }
  }
}
```

## API Endpunkte

### Welten anlegen

**POST /worlds**

Role: `CREATOR`

Erstellt eine neue Welt. Die ID wird automatisch generiert.

### Welten abfragen

**GET /worlds/{id}**

Role: `USER`.

Fragt eine Welt anhand ihrer ID ab. Gibt die Metadaten der Welt zurück.

### Welten auflisten

**GET /worlds**

Role: `USER`.

Listet alle verfügbaren Welten auf. Optional können Filterparameter
angegeben werden, um die Ergebnisse einzuschränken.

Es werden maximal 100 Welten pro Anfrage zurückgegeben. Die Ergebnisse können
mit den Parametern `page` und `size` paginiert werden.

Maximum `size` ist 100, Standard ist 20.

### Welten aktualisieren

**PUT /worlds/{id}**

Role: `ADMIN` oder owner der Welt.

Aktualisiert die Metadaten einer bestehenden Welt. Die ID der Welt muss in der URL angegeben werden.
Die Anfrage muss die aktualisierten Metadaten im Body enthalten.

### Welten löschen

**DELETE /worlds/{id}**

Role: `ADMIN` oder owner der Welt.

Löscht eine bestehende Welt. Die ID der Welt muss in der URL angegeben werden.
Die Welt wird aus der Datenbank entfernt und kann nicht wiederhergestellt werden.

### Enable/Disable Welt

**POST /worlds/{id}/enable**
**POST /worlds/{id}/disable**

Role: `ADMIN` oder owner der Welt.

Aktiviert oder deaktiviert eine bestehende Welt. Die ID der Welt muss in der URL angegeben werden.
