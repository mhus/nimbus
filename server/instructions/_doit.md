

Erstelle im modul shared eine entity fuer keys: SKey mit SKeyRepository und 
SKeyService der PublicKeyProvider, PrivateKeyProvider und SymmetricKeyProvider 
implementiert, in SKey soll es geben: type:string und kind: (private, public, symmetric), 
algorithm:string, name:string, key:string, createdAt

```text
Im Modul shared wurde eine persistierbare Entity SKey mit Repository und Service implementiert. Der Service erfüllt die Interfaces PublicKeyProvider, PrivateKeyProvider und SymmetricKeyProvider und konvertiert Base64-encodierte Schlüssel in Java-Key-Objekte. Das Projekt baut erfolgreich und die Provider-Implementierung steht als Spring-Bean bereit.
```
---

Erstelle im modul universe Rest Controller um keys zu verwalten.
unter /universe/user/keys

Nur UUser mit der Rolle ADMIN darf auf diese Endpunkte zugreifen.
Erstelle Endpunkte zum suchen / auflisten, erstellen, bearbeiten (nur name), löschen.
Der key selbst darf nie zurueckgegeben werden.
Erstelle OpenAPI Annotationen.

```text
Ein REST-Controller für Schlüsselverwaltung wurde im Modul universe unter /universe/user/keys implementiert. Nur Benutzer mit ADMIN-Rolle haben Zugriff auf die Endpunkte, die Schlüssel auflisten, erstellen, bearbeiten (nur Name) und löschen. Das Key-Material wird nie zurückgegeben und alle Endpunkte sind mit OpenAPI annotiert.
```
---

Ich brauche eine Entity UWorld mit URepsoitory und UWorldService im modul universe. Die UWorld soll folgende Felder haben:
- name:string
- worldId:string
- description:string
- createdAt:Date
- regionId:string
- planetId:string
- solarSystemId:string
- galaxyId:string
- coordinates:string

Ich brauche einen REST Controller unter /universe/user/world mit dem ich UWorlds verwalten kann.
Nur ADMIN-Benutzer haben Zugriff auf diesen Endpunkt.

Es werden noch die felder:
- worldId:string
- coordinates:string
an UWorld benoetigt.
---

Es wird ein weiterer REST Controller unter /universe/region/{regionId}/world/{worldId} benoetigt.
Hier verwalten die regionen automatisch welten.
- Zugriff nur mit einem Bearer Token der regionId, pruefe mit JwtService, als owner im key wird die regionId genutzt.
- Erstelle create, update und delete Endpunkte.
Du kannst auch einen SecurityFilter auf /universe/region/{regionId} erstellen der immer den Token prueft.

Erstelle eine GET roue fuer /universe/region/{regionId}/world/{worldId}

---

Erstelle im Modul 'region' einen RUniverseService mit dem die region auf die rest routen
des universe servers auf /universe/region/{regionId}/world/{worldId} einfach zugreifen kann.
