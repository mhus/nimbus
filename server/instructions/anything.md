
# Anything

[?] Erstelle in world-shared eine entity WAnythingEntity, WAnythingRepsoitiry, WAnythingService mit den folgenden Komponenten:
- reginonId // separate region to search for - optional
- worldId // worldId to search for - optional
- collection // collection to search for
- name
- description
- data : Object
- type : String
- createdAt : Date
- updatedAt : Date

[?] Erstelle in world-control einen Rest Controller mit dem ueber WAnythingService die entity verwaltet werden kann.

[?] Unter client/packages/shared/src/generated/entities/WAnythingEntity.ts ist nun die entsprechende entity in TypeScript vorhanden.
- Erstelle in client/packages/controls einen anything-editor.html
- Als vorlage kann asset-editor.html dienen
- In diesem Fall soll oben rechts ein region-selector angezeigt werden. Die welt ist optional neben der collection suchbar.
- Erweitere ggf den rest controller wegen der suche.

[ ] Erstelle in HomeApp.vue einen Eintrag fuer den neuen Editor
