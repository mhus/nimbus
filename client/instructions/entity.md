
# Entity

Entities sind dynamische Objekte, die in der Spielwelt existieren. Sie wechseln oft ihre position, ausrichtung oder ihren 
Zustand. Beispiele für Entitäten sind Spieler, NPCs (Nicht-Spieler-Charaktere), Fahrzeuge und bewegliche Gegenstände.

## Entity Model

EntityModel:
- id
- type
- modelPath
- positionOffet : Vector3
- rotationOffet : Vector3
- poseMapping : Map<int, string>
- poseType : PoseType // '2-Legs', '4-Legs', '6-Legs', 'Wings', 'Fish', 'Snake', 'Humanoid', 'Slime'
- modelModifierMapping : Map<string, string>
- dimensions: {
    walk: { height: number; width: number; footprint: number };
    sprint: { height: number; width: number; footprint: number };
    crouch: { height: number; width: number; footprint: number };
    swim: { height: number; width: number; footprint: number };
    climb: { height: number; width: number; footprint: number };
    fly: { height: number; width: number; footprint: number };
    teleport: { height: number; width: number; footprint: number };
    };

Entity:
- id : string - unique identifier
- name : string
- model : EntityModel
- modelModifier : Record<string, any>
- movementType : 'static' | 'passive' | 'slow' | 'dynamic'
- solid? : boolean
- interactive?: boolean // Gibt an, ob die Entität interaktiv ist (kann angeklickt/benutzt werden)

EntityPathway:
- entityId : string
- startAt : timestamp
- waypoints : Waypoint[],
- isLooping? : boolean - ??? - recalculate waypoints with startAt
- queryAt? : timestamp
- idlePose?

Waypoint:
- timestamp : number - target timestamp
- target : Vector3
- rotation : direction, pitch
- pose : int

## Netzwerk

- Wurde im network-model-2.0.md beschrieben.
- Erweiterung von Ping um das Server Lag des timestamps zu ermitteln

## Umsetzung

[?] Im NetworkService soll der ping jetzt oefters ausgefuehrt werden (variable die mit getter setter angepasst werden kann) und das RTT un das timestamp Lag zum server bereitgestellt werden. 

[?] Es soll ein EntityService geben, der die entitys und entity modelle läd, cached und verwaltet
- Er nutzt den NetworkServie um die URL zur REST API zu bekommen
- Er laed die entity modelle und entitys lazy wenn benoetigt
- Wurde eine entity oder model lange nicht genutzt, wird es aus dem cahe entfernt
- Im EntityService wird ein ClientEntity typ zum cachen genuzt.

ClientEntity
- id : string
- model : EntityModel
- entity: Entity
- visible : boolean
- meshes : Mesh[]
- currentPosition : Vector3
- currentRotation : Vector3
- currentWaypointIndex : int
- currentPose : int
- currentWaypoints : Waypoint[]
- lastAccess : timestamp

Der EntityService soll im AppContext verfuegbar sein 

[?] Im ModelRenderer werden modelle geladen und gerendert. Es soll einen ModelService geben, der im EngineService referenziert wird. 
Er laed Modelle, cacht diese und stellt sie zum rendern bereit. Der ModelRenderer soll den ModelService nutzen um die modelle zu laden.

[?] Es soll einen EntityRenderService geben, der die entitys in der welt rendert. Er wird vom EntityService benachrichtigt 
wenn eine entity erscheint oder verschwindet. Sich die position, rotation oder die Pose aendert.
- Modelle koenne vom ModelService geladen werden
- EntityRenderService soll im EngineService referenziert sein

[?] Im EntityService soll es einen Loop geben, der alle 100 (constante) ms die positionen und status der entitys aktualisiert
- Kommt ein unbekannter entity pathway an, wird die entity und ggr entitymodel vom server geladen und ein ClientEntity objekt erstellt.
- Die position der entity wird anhand der waypoints und des timestamps berechnet
- Wenn die entity in der naehe (radius variable im EntityService - getter, setter) des spielers ist, wird sie 'visible' gestellt und an den EntityRenderService uebergeben um gerendert zu werden
- Wenn die entity ausserhalb der sichtweite des spielers ist, wird sie aus dem EntityRenderService entfernt
- Wenn die entity lange nicht genutzt wurde, wird sie aus dem cache entfernt
- Updates aus dem Netwerk werden in den ClientEntity objekten verarbeitet und die position, rotation und pose aktualisiert
- Es wird das timeLag aus dem NetworkService genutzt um die position der entitys korrekt zu berechnen

[?] Wird ein chunk entfernt, werden alle entitys die in diesem chunk gerendert auf visible=false gesetzt und entfernt, damit keine leichen in der welt bleiben

[?] Erstelle mir ein Commando in engine, mit dem ich infos ueber geladenene entitys bekommen kann, z.b. listEntities und entityInfo {entityId}
[?] In EntityModel wird ein weiterer parameter scale : Vector3 hinzugefuegt, der die skalierung des modells angibt
- Scalierung soll dann beim rendern im EntiryRenderService beruecksichtigt werden

[ ] Erstelle mir ein Commando in engine, mit dem ich testweise eine neue Entity in der Welt spawnen kann, z.b. spawnentity {id} {entityModelId} {x} {y} {z}

### Server

[?] Im Server muss es ein Entity Management geben, der die entitys verwaltet
- EntityModelle werden als json in files/entitymodels/ gespeichert
- Entitys werden im speicher gehalten, erstelle dazu einen EntityManager im test_server package
- Erstelle die REST Endpunkte zum laden der entity modelle und entitys (siehe client/instructions/general/server_rest_api.md)
  GET /api/worlds/{worldId}/entitymodel/{entityModelId}
  GET /api/worlds/{worldId}/entity/{entityId}
- Es soll ein EntitySimulator geben, der entitys in der welt spawnt und deren pathways verwaltet, definitionen dazu sind auf dem filesysem
  im data/worlds/{worldId}/entities/ zu finden. Hier werden entities definitionen als json gespeichert.

ServerEntitySpawnDefinition:
- entityId : string
- entityModelId : string
- initialPosition : Vector3
- initialRotation : Vector3
- middlePoint : Vector3
- radius : number
- speed: number
- behaviorModel: string // reference auf einen simulations algorithmus
- currentPathway? : EntityPathway
- chunks : Vector2[] // list of chunk positions where this entity is active in the pathway

- Behavior
  - PreyAnimalBehavior - bewegt sich langsam im radius um den mittelpunkt, erstelle alle 5 sekunden neue Pathways die an den alten anschliessen
- Aus dem Pathway wird einmal die liste der betroffenen chunks ermittelt und das dann an die client sessionhandler geschickt.

- Erstelle ein Behavior System fuer die entity simulation, das verschiedene verhaltensweisen umsetzen kann

[?] Server Entity per Session verwalten
- Es werden nur neue Pathways zum client gesendet, wenn er sich fuer den chunk angemeldet hat in dem diese statt finden.
- Wird irgendwo ein neuer pathway erzeugt wird dieser an alle sessions handler im server gesendet und die session prueft ob die entity bewegung in einem der client chunks stattfindet.
- Entity Pathways werden gesammelt und alle 100ms an den client gesendet und die queue geloescht.

[?] Ich moechte fuer Entities, managed by EntityService auch eine leichte physik umsetzen, nicht so komplex wie fuer den lokalen player. Wie kann ich das machen? 
```text
     Architektur-Überblick

     - Server: Berechnet Physik (Gravitation + Block-Kollision), generiert physik-basierte Pathways
     - Client: Empfängt Pathways als "Prediction Hints", führt leichte Physik für smooth interpolation aus
     - Performance: Distance-based update rates (nah: 60 FPS, mittel: 30 FPS, fern: 10 FPS)

     1. Datenstruktur-Erweiterungen

     EntityData.ts (shared):
     - Entity Interface: physics?: boolean Flag hinzufügen
     - EntityModel Interface: Physik-Eigenschaften hinzufügen (Masse, Friction-Werte)
     - EntityPathway: physicsEnabled?: boolean Flag für Physik-Pathways
     - ServerEntitySpawnDefinition: Physik-State (velocity, grounded)

     2. Server-Side Physik (EntitySimulator)

     Neue Komponenten:
     - EntityPhysicsSimulator.ts: Leichte Physik-Engine für Entities
       - Gravitation (vereinfacht, konstante -20.0)
       - Block-Kollision (X/Y/Z-Achsen separat)
       - Ground-Detection via WorldManager
       - Velocity-State pro Entity

     EntitySimulator Erweiterungen:
     - Physics-Update neben Behavior-Update
     - Physics-basierte Pathway-Generierung
     - Update-Rate: 20 TPS (Ticks per Second) für Physik

     3. Client-Side Physik (EntityService)

     Neue Komponenten:
     - EntityPhysicsController.ts: Client-seitige Physik-Interpolation
       - Empfängt Server-Pathways als Predictions
       - Führt lokale Physik aus (Gravitation + Kollision)
       - Korrigiert bei Abweichungen vom Server

     EntityService Erweiterungen:
     - Distance-based Update-Raten:
       - < 20 Blöcke: 60 FPS (jeder Frame)
       - 20-40 Blöcke: 30 FPS (jeder 2. Frame)
       40 Blöcke: 10 FPS (jeder 6. Frame)
     - Physics-State pro Entity (velocity, grounded)
     - Integration mit bestehendem Waypoint-System

     4. Physik-Implementation Details

     Gravity: -20.0 blocks/s² (wie Player)
     Collision: Simplified AABB gegen Blocks
     Friction: Ground friction 0.8 (velocity dampening)
     Integration: Verlet oder Euler (je nach Stabilität)

     5. Behavior-Integration

     PreyAnimalBehavior anpassen:
     - Falls entity.physics === true: Nutze Physik statt fixer Waypoints
     - Behavior gibt nur "Intention" (Zielrichtung), Physik führt aus

     6. Test-Entities

     Bestehende Entities updaten (cow1, pig1, farmer1):
     - physics: true Flag setzen
     - Testen mit verschiedenen Terrain-Situationen

     Dateien, die geändert werden:

     1. shared/src/types/EntityData.ts - Datenstrukturen
     2. test_server/src/entity/EntitySimulator.ts - Server-Physik
     3. test_server/src/entity/EntityPhysicsSimulator.ts - NEU
     4. test_server/src/entity/behaviors/PreyAnimalBehavior.ts - Physik-Integration
     5. engine/src/services/EntityService.ts - Client-Physik
     6. engine/src/services/entity/EntityPhysicsController.ts - NEU
     7. Entity JSON files (cow1, pig1, farmer1) - Physics flag

     Geschätzter Aufwand: 3-4 Stunden

⏺ Perfekt! Ich starte mit der Implementierung. Lass mich zuerst die Todo-Liste erstellen:

⏺ Jetzt starte ich mit der ersten Aufgabe. Lass mich zuerst die aktuelle EntityData.ts lesen:
```

[ ] Es soll auch collisions mit entitys geben. Wenn der Player mit einer entity kollidiert, soll er nicht durch diese hindurch 
gehen koennen.
- Betroffen sind nur entities mit dem parameter solid=true
- Die collision soll im Client umgesetzt werden, dazu muss im EntityService die bounding box der entity
    berechnet und im PhysicsService fuer die collision detection mit dem player genutzt werden.
- Die bounding box der entity wird aus der position der entity und den dimensionen des entity models und der rotation der entity berechnet.
- Die dimensionen des entity models werden anhand des aktuellen movement types des players ausgewählt (walk, sprint, crouch, swim, climb, fly, teleport)
- Die collision detection soll im PhysicsService umgesetzt werden, dort wird die bounding box der entityn
    gegen die bounding box des players getestet.
- Wenn eine collision erkannt wird, wird die position des players so angepasst, dass er nicht mehr in der bounding box der entity ist.
- Es werden nur entities geprueft, die in der naehe des players sind (radius variable im EntityService - getter, setter)

[ ] Der Client soll die Positionsdaten des Players zum Server senden. Dazu soll er ca. alle 100ms ein Update an den Server 
schicken mit der aktuellen Position, Rotation und Bewegungstyp des Players. Natürlich nur, wenn sich die Position oder 
Rotation seit dem letzten Update geändert hat. 
- Beim senden des Updates wird eine Bewegung ueber die kommenden 200ms vorhergesagt (basierend auf der aktuellen Geschwindigkeit und Bewegungstyp)
- Der Server kann diese Daten nutzen um die Position des Players an andere Player zu senden
- Der Server sendet alle 100ms alle relevanten, neuen Player Positionen an den Client, wenn der diese benoetigt (registrierte chunks)
- Der Server muss per REST API 'GET /api/worlds/{worldId}/entity/{entityId}' Daten des players bereit stellen, damit der Client die Player Entity laden kann
  player entitys beginnen immer mit einem '@' zeichen.
- Eine vorlage fuer die Player Entity ist in 'files/entity/player_entity.json' zu finden, es muss aber noch die id des players angepasst werden.

[ ] Erstelle einen neuen EntitySimulator im test_Server. Der TestFastEntitySimulator soll entities mit schnellen bewegungen simulieren.
- Wie ein echter Player, der immer wieder seine Richtung und Geschwindigkeit aendert
- Es wird immer eine kurze pathway mit 1 pathway punkt erstellt, der die naechste position des entitys angibt
- Die pathway wird alle 100ms neu erstellt und neu gesendet (konfigurierbar)
- Die geschwindigkeit des entitys soll zwischen 1 und 2 blocks per sekunde liegen (konfigurierbar)

[ ] Waypoints Zeichnen mit startpunkt aktuelle position der entity
[2025-11-12T20:56:23.620Z] [DEBUG] [EntityRenderService] Not enough waypoints to draw lines
Data: {
"entityId": "farmer1",
"count": 1
}
Das sollte dann nur noch passieren, wenn 0 waypoints vorhanden sind.

[x] Wenn ein neuer pathway vom server kommt, sollen alle aktuellen pathways der entity verworfen werden und nur der neue pathway genutzt werden.
Siehe EntityService.ts

[ ] Wenn ein neuer Pathway kommt und die velocity zum erste punkt zu hoch wird, soll die entity zum ersten punkt flippen dann sollden
die waypoints normal abgearbeitet werden.

