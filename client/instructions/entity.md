
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

## TODO

[?] Im NetworkService soll der ping jetzt oefters ausgefuehrt werden (variable die mit getter setter angepasst werden kann) und das RTT un das timestamp Lag zum server bereitgestellt werden. 
