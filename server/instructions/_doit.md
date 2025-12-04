

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

```text
Ein RUniverseService wurde im Modul region implementiert, der REST-Routen des Universe-Servers unter /universe/region/{regionId}/world/{worldId} zugänglich macht. Der Service nutzt einen konfigurierbaren RestTemplate-Client und erstellt automatisch ein Region-Bearer-Token. Das Projekt wurde erfolgreich gebaut ohne Kompilationsfehler.
```

---

In module region werden sollen welten verwaltet/registriert werden in der Entity RWorld mit RWorldRepository und RWorldService.
RWorld:
- name:string
- worldId:string
- description:string
- createdAt:Date
- regionId:string
- worldApiUrl:string

---

Erstelle im modul 'region' rest controller um welten zu verwalten.
- Unter /region/{regionId}/world/{worldId}
- Zugriff nur mit einem Token, der mit dem≥ world  Key erzeugt wurde.
  - du kannst einen SecurityFilter auf /region/{regionId}/world/{worldId} erstellen. 
- Erstelle create, update, get und delete Endpunkte.

---

Erstelle im module 'world' einen WRegionService mit dem der world server auf die endpunkte
vom region server zugreifen kann.

---

Solltend die DTOs zum kommunizieren via REST zwischen den servern nicht ie gleichen sein?
Die koennen im modul shared abgelegt sein.

----


- Umstellen MongoStorageService
  - Storage immer aus diesem ladne, nicht mehr in content

- chunk refresh event nicht mehr vom life aus senden, sondern mit sheduler, life hat dann einen timeout auf den chunks

- Direkte Kommunikation zwischen world servern via REST, player sendet siene direkte IP mit
- Commands weiter geben von player an weitere server (life und control). command schema?
  - life.xyz, control.xyz 
  
- Edit Mode im player
  - An der session
  - Updated blocks von redis laden
  - Blocks werden von control direct in redis geschrieben
  - Bei ende der session migrieren
  - Bei ende Edit mode migrieren

- Umbennenen worldId in worldUid
  - worldId: main
  - worldUid: region:worldId$zone:branch
  - worldInstanceId: region:worldId$zone:branch@instance

