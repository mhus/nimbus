
# Material Editor implemetation

Der Material Editor ist eine Webapplikation mit

TypeScript + Vite + Vue3 + tailwind css

Er erlaubt es Block Typen, Texturen, Assets zu erstellen, zu bearbeiten und zu speichern.

## Server

[x] Im Server werden REST Endpunkte benötigt um Block Typen zu bearbeiten
die üeber die Definition in 'client/instructions/client_2.0/server_rest_api.md' hinausgehen.
- GET /api/worlds/{worldId}/blocktypes?query={query}
- POST /api/worlds/{worldId}/blocktypes
Response: {id: number}
- PUT /api/worlds/{worldId}/blocktypes/{blockTypeId}
- DELETE /api/worlds/{worldId}/blocktypes/{blockTypeId}

- Erstelle die Endpunkte
- Erweitere die Dokumentation in 'client/instructions/client_2.0/server_rest_api.md'

[x] Im Server werden REST Endpunkte benötigt um Assets zu bearbeiten
die über die Definition in 'client/instructions/client_2.0/server_rest_api.md' hinausgehen.
- GET /api/worlds/{worldId}/assets?query={query}
- POST /api/worlds/{worldId}/assets/{neuerAssetPath}
- PUT /api/worlds/{worldId}/assets/{assetPath}
- DELETE /api/worlds/{worldId}/assets/{assetPath}

## Setup

[?] Erstelle ein neues Packet material_editor das auch die sourcen in 'shared' nutzt.
- Richte Vite mit Vue3 und tailwind css ein.
- Erstelle die Grundstruktur der Applikation mit den benötigten Komponenten.
- In einer dotnet Datei wird die aktuelle API_URL fest hinterlegt.
- Rest API Dokumentation: 'client/instructions/client_2.0/server_rest_api.md'
- Im Header steht die aktuelle 'World', diese ist aktuell immer 'main' (in dotnet Datei WORLD_ID) und kann aber durch click geändert werden.
  - Bei Click wird im Server 'GET /api/worlds' aufgerufen und die worlds angezeigt. 
  - Die ausgewahlte World wird übernommen.
- Es werden zwei Teile benötigt (Tabs?): Block Type Editor und Texture + Asset Editor. 
- Block Type Editor:
  - Liste der Block Types (Block Types) mit Suchfunktion
  - Button um neuen Block Type zu erstellen und zu löschen.
  - Auswahl eines Block Types öffnet den Editor
  - DTO ist in 'client/packages/shared/src/types/BlockType.ts' und 'client/packages/shared/src/types/BlockModifier.ts'
  - Die Parameter können bearbeite und wieder auf 'null' gestellt werden.
  - Es können mehrere 'status' Einträge erstellt und bearbeitet werden. Die Editoren werden dann untereinander in einer Liste angezeigt.
- Texture + Asset Editor:
  - Liste der Textures und Assets mit Suchfunktion, wenn ein bekanntes Format (Bilddatei, png, ...) dann Preview anzeigen
  - Button um neue Textures und Assets zu erstellen und zu Löschen.
  - Es gibt keinen richtigen Editor, da es sich um Binärdaten handelt.

```text
  🚀 Anwendung starten:

  # Von Root-Verzeichnis aus:
  cd /Users/hummel/sources/mhus/nimbus/client

  # Material Editor starten (Port 3002):
  pnpm dev:material_editor

  # Oder direkt im Package:
  cd packages/material_editor
  pnpm dev

  🔧 Build für Production:

  # Von Root:
  pnpm build:material_editor

  # Oder direkt:
  cd packages/material_editor
  pnpm build

  📝 Konfiguration:

  Die Datei /packages/material_editor/.env.local enthält:
  - VITE_API_URL=http://localhost:3000 (Server URL)
  - VITE_WORLD_ID=test-world-1 (Default World)
```

## Block Type Editor

[ ] Das Editieren der status Sections soll besser untertuetzt werden.
- Du kannst einen neuen Dialog aufmachen im einene Status zu bearbeiten.
- Referenz in 'client/packages/shared/src/types/BlockModifier.ts'
- Die Bereiche sollen ausklappbar sein. Sind sie eingeklappt, wird das optionale Feld als Empty gespeichert (Daten gehen verloren) - Oder noch schöner, neben dem Titel des bereichs ist eine checkbox, die steuert ob der Bereich dann empty wird.
- Erstelle einen Bereich für 'visibility' und Biete Elemente an zum Bearbeiten (chckboxen, textfelder, dropdowns, ...)
  - Die IDs für Texturen sind normiert, siehe Type Docu 'TextureKey'
  - Texturen koennen zwei moeglichkeiten haben: Entweder direkt der Pfad oder ein Objekt mit vuModifier, etc (siehe TextureDefinition)
- Erstelle einen Bereich für 'physics' und Biete Elemente an zum Bearbeiten (chckboxen, textfelder, dropdowns, ...)
- Erstelle einen Bereich für 'wind'.
- Erstelle ein Dropdown für 'effect'
- Erstelle ein textFeld für 'spriteCount'
- Erstelle ein textFeld für 'alpha'
- Erstelle einen Bereich für 'illumination'
- Erstelle einen bereich für 'sound'