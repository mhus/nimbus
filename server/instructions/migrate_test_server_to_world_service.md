
# World Service

## Migration

Die Aufgabe ist es die inbound funktionalitaet des TypeScript test_server packages zu migrieren.
- Pfad zum test_server: ../client/packages/test_server/src
- Der test Server nutzt Typen in ../client/packages/shared/src
- Es sollen alle rest endpunkte mit GET migriert werden (kein POST, PUT, DELETE).
- Als Persistierung wird mongoDB genutzt. Es gibt Services (Beans) fuer die verschiedenen Entity Typen.
- Für messaging wid redis benutzt.
- Alle typen in ../client/packages/shared/src sind im maven modul generated bereits migriert, diese typen können nicht 
  angepasst werden und dienen als contract.

Alle Migrationen in das Modul world-shared und world-player.

Lets go:

[ ] Alle Daten-Services erstellen, JPA Entities, Repository, Service
- schon vorhanden: AssetEntity, AssetRepository, AssetService (world-shared)
- noch benoetigt: BlockTypeEntity, BlockTypeRepository, BlockTypeService (world-shared)
- noch benoetigt: ModelTypeEntity, ModelTypeRepository, ModelTypeService (world-shared)
- noch benoetigt: BackDropEntity, BackDropRepository, BackDropService (world-shared)
- noch benoetigt: EntityType, EntityRepository, EntityService (world-shared)
- noch benoetigt: EntityModelEntity, EntityModelRepository, EntityModelService (world-shared)
- noch benoetigt: ItemTypeEntity, ItemTypeRepository, ItemTypeService (world-shared)

[ ] Alle REST Endpoints erstellen (world-player)
- Rest Endpunkte, siehe ../client/instructions/general/server_rest_api.md

Migration der WebSocket Messages (world-player)
- wenn events auf andere sessions verteilt werden muessen, dann redis nutzen.
- Network Messages, siehe ../client/instructions/general/network-model-2.0.md
- Die WebSocket Session wird stateful gehalten. Bei einem Disconnect geht der Session-Status in DEPRECATED, nutxe WSockedService

[ ] Login Message implementieren (Client -> Server)
- aktuell wird login implementiert, aber noch nicht validiert, wird nicht bleiben, deprecated
- Wichtig, login mit sessionId wird bleiben
[ ] Ping (Client -> Server)
- WorldService - setStatus - wird in mongoDb gespeichert
[ ] Chunk Registration (Client -> Server)
- Wird in Websocket session gehalten
[ ] Chunk Anfrage (Client -> Server)
[ ] Block Interaction (Client -> Server)
[ ] Entity Position Update (Client -> Server)
[ ] Entity Interaction (Client -> Server)
[ ] Animation Execution (Client -> Server)
[ ] User Movement Update (Client -> Server)
[ ] Interaction Request (Client -> Server)
[ ] Client Command (Client -> Server)
[-] Logout (Client -> Server) 
- Deprecated?
[ ] Effeckt Trigger (Client -> Server)
[ ] Effect Update (Client -> Server)

Server sendet messages:
[ ] Update world status (Server -> Client)
[ ] Chunk Update (Server -> Client)
[ ] Block Update (Server -> Client)
[ ] Item Block Update (Server -> Client)
[ ] Block Status Update (Server -> Client)
[ ] Entity Chunk Pathway (Server -> Client)
[ ] Animation Execution (Server -> Client oder Client -> Server)
[-] Player Teleport (Server -> Client) - wird nicht umgesetzt, spaeter mit einem Engine-Command
- Deprecated
[ ] Server Command (Server -> Client)
- ServerCommandService
[ ] Multiple Commands (Server -> Client)
[ ] Effeckt Trigger (Server -> Client)
[ ] Effect Update (Server -> Client)
[ ] Team Data (Server -> Client)
[ ] Team Status (Server -> Client)

Server Side Commands:
[ ] Migration von Server side Commands (world-player)
- HelpCommand
- ItemCommand
- LoopCommand
- NavigateSelectedBlockCommand
- SetSelectedEditBlockCommand
- TeamDataCommand
- TeamStatusCommand
- WorldCommand
