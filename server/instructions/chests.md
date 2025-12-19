
# Chests

Chests sind Ablagen für Items. Sie können User Spezifisch oder Generel sein. Sie werden aber vom gleichen Service und UI
verwendet. Sie sind immer region spezifisch, da auch Item region spezifisch sind.

## Anlegen

[ ] Erstelle eine WChest und WChestRepository und WChestService in world-shared
WChest
- regionId: String - Separate RegionId aus der WorldId, ist immer gesetzt!
- worldId: String - Optional.
- name: String - interner character, z.b. uuid
- displayName: String - Optional.
- description: String
- userId: String - Optional
- type: Region, World, User
- items: List<Item>
- createdAt: Date
- updatedAt: Date

[ ] Erstelle eine WChestController in world-api zum verwalten von WChests in world-control
- Abfrage fuer user related chests
- Abfrage fuer region related chests
- Abfrage fuer world related chests
