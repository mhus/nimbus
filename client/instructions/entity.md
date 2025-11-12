
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
[ ] In EntityModel wird ein weiterer parameter scale : Vector3 hinzugefuegt, der die skalierung des modells angibt
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

