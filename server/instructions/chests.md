
# Chests

Chests sind Ablagen für Items. Sie können User Spezifisch oder Generel sein. Sie werden aber vom gleichen Service und UI
verwendet. Sie sind immer region spezifisch, da auch Item region spezifisch sind.

## Anlegen

[?] Erstelle eine WChest und WChestRepository und WChestService in world-shared
WChest
- regionId: String - Separate RegionId aus der WorldId, ist immer gesetzt!
- worldId: String - Optional.
- name: String - interner character, z.b. uuid
- displayName: String - Optional.
- description: String
- userId: String - Optional
- type: Region, World, User
- items: List<ItemRef>
- createdAt: Date
- updatedAt: Date

[?] Erstelle eine WChestController in world-api zum verwalten von WChests in world-control
- Abfrage fuer user related chests
- Abfrage fuer region related chests
- Abfrage fuer world related chests

[?] Unter client/packages/shared/src/generated/entities/WChest.ts ist nun die TypeScript entity.
- Erstelle in client/packages/controls ein chest-editor.html um chests zu verwalten.
- Als vorlage kann asset-editor.html dienen
- In diesem Fall soll oben rechts ein region-selector angezeigt werden.
- Erweitere ggf den rest controller wegen der suche.

[?] Aenderung: in WChest wurde
- private List<Item> items = new ArrayList<>();
in
- private List<ItemRef> items = new ArrayList<>();
geandert. das anedert viel.
- Starte mit anpassung in WChestRepository und WChestService und Controller

[?] Es gibt jetzt ein client/packages/shared/src/generated/entities/WItem.ts TypeScript Entity.
Diese sollte in world-control EItemController (java WItem) ausgeleifert werden und in item-editor.html verwendet werden.
- pruefen ob WItem oder nur Item ausgeleifert wird
- item-editor.html anpassen

[?] Der chest-editor muss neu gemacht werden. Da wir referenzen auf Items haben muessen auch items geladen werden (EItemController).
Items weiden auf der einen Seite geladen und Chest auf der anderen Seite.
Items koennen nun im chest referenziert werden (->) oder im chest wieder entfernt. bzw. anzahl bearbeitet werden.

[?] Erstelle in HomeApp.vue einen Eintrag fuer den neuen chest-editor Editor
